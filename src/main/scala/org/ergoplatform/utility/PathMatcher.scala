package org.ergoplatform.utility

import cats.effect.Resource
import cats.implicits._
import com.joefkelley.argyle._
import org.ergoplatform.wallet.mnemonic.Mnemonic
import org.ergoplatform.wallet.secrets.{DerivationPath, ExtendedSecretKey}
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import zio._
import zio.interop.catz._

import scala.util.Try
import scala.io.Source

/** Traverses keys tree matching corresponding derivation path to address.
  */
object PathMatcher extends App {

  val TestnetPrefix: Byte = 0x10
  val MainnetPrefix: Byte = 0x00

  def checkLeafsAtRange(
    env: TaskEnv,
    basePath: List[Int],
    minLeafIdx: Int,
    maxLeafIdx: Int
  )(implicit ae: ErgoAddressEncoder): Option[DerivationPath] = {
    @scala.annotation.tailrec
    def loop(leafIdx: Int): Option[DerivationPath] =
      if (leafIdx <= maxLeafIdx) {
        val path =
          DerivationPath(basePath :+ leafIdx, publicBranch = false)
        if (addressFromPath(path, env.rootSk) == env.address) Some(path)
        else loop(leafIdx + 1)
      } else {
        None
      }
    loop(minLeafIdx)
  }

  def checkSubtree(
    env: TaskEnv,
    basePath: List[Int],
    leafsNum: Int,
  )(implicit ae: ErgoAddressEncoder): UIO[Option[DerivationPath]] = {
    def checkNext: UIO[Option[DerivationPath]] =
      if (basePath.size > 1 && basePath.last < leafsNum)
        checkSubtree(
          env,
          basePath.init :+ basePath.last + 1,
          leafsNum
        )
      else
        checkSubtree(env, basePath :+ 0, leafsNum)
    val windowSize = (leafsNum.toFloat / env.numCores).ceil.toInt
    (0 to leafsNum)
      .grouped(windowSize)
      .toList
      .parTraverse { range =>
        UIO {
          checkLeafsAtRange(env, basePath, range.head, range.last)
        }
      }
      .flatMap {
        _.collectFirst { case Some(path) => path }
          .fold(checkNext)(r => UIO.succeed(Some(r)))
      }
  }

  def addressFromPath(path: DerivationPath, rootSk: ExtendedSecretKey)(
    implicit ae: ErgoAddressEncoder
  ): String =
    P2PKAddress(
      rootSk.derive(path).asInstanceOf[ExtendedSecretKey].publicKey.key
    ).toString()

  def task(
    env: TaskEnv
  )(implicit ae: ErgoAddressEncoder): UIO[Option[String]] =
    checkSubtree(env, List(0), env.leafsNum).map(_.map(_.encoded))

  def program(args: Array[String]): ZIO[Any, Throwable, Unit] =
      IO.fromTry(argParser.parse(args))
        .flatMap { args =>
          val ae = ErgoAddressEncoder(args.networkPrefix)
          IO.fromTry(ae.fromString(args.address)).flatMap { _ =>
            Resource
              .fromAutoCloseable(IO(Source.fromFile(args.mnemonicPath)))
              .use(s => IO(s.getLines().next()))
              .flatMap { mnemonic =>
                val seed = Mnemonic.toSeed(mnemonic)
                val rootSk = ExtendedSecretKey.deriveMasterKey(seed)
                IO.fromTry(Try(java.lang.Runtime.getRuntime.availableProcessors))
                  .flatMap { numCores =>
                    val env = TaskEnv(args.address, rootSk, args.leafsNum, numCores)
                    task(env)(ae).flatMap {
                      case Some(p) => UIO(println(s"Derivation path is: $p"))
                      case _       => UIO(println("Nothing found"))
                    }
                  }
              }
          }
        }

  def run(args: List[String]): ZIO[PathMatcher.Environment, Nothing, Int] =
    program(args.toArray).fold(_ => 1, _ => 0)

  def argParser: Arg[Args] =
    (
      required[String]("--mnemonicPath", "-mp") and
      required[String]("--address", "-a") and
      required[Int]("--numLeafs", "-l") and
      requiredOneOf[Byte](
        "--mainnet" -> MainnetPrefix,
        "--testnet" -> TestnetPrefix
      )
    ).to[Args]

  final case class TaskEnv(
    address: String,
    rootSk: ExtendedSecretKey,
    leafsNum: Int,
    numCores: Int
  )

  final case class Args(
    mnemonicPath: String,
    address: String,
    leafsNum: Int,
    networkPrefix: Byte
  )

}
