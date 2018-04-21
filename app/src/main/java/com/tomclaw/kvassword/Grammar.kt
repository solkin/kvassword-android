package com.tomclaw.kvassword

class Grammar(val startBiGram: Array<String>,
              val lookupBiGram: Array<String>,
              val nextCharLookup: Array<Array<Array<String>>>)
