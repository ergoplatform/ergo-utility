# Ergo Utility

Ergo Utility is a set of useful utilities for Ergo protocol.

Currently available utilities:

- Path matcher
    
    Can be used in case you've lost derivation path from one of your addresses.
    
    Class path: `org.ergoplatform.utility.PathMatcher`
    
    Arguments:
    - `--menmonicnPath, -mp` - a path to file with menmonic phrase of your wallet
    - `--address, -a` - address corresponding to the lost derivation path
    - `--numLeafs, -a` - number of leafs to check in each keys subtree
    - `--mainnet or --testnet` - address type
    
    Run example: `java -cp [path_to_jar] org.ergoplatform.utility.PathMatcher -mp /path/to/mnemonic.txt -a 9fHB7R2bk21pLUwU2rfA5zvyLfmmPNoKea35Rj5uCeE5dAS2Jeo -l 100 --mainnet
`
