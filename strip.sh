#!/bin/bash

java -Xss2m -ea -cp "$(dirname $0)/build/libs/RedPEG.jar":./ \
  i2.act.main.StripTokens "$@"
