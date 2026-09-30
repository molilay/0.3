#!/bin/bash
export JAVA_HOME=$HOME/tools/jdk-17.0.20.1+1
export PATH=$JAVA_HOME/bin:$HOME/tools/gradle-8.9/bin:$PATH
cd /home/user/UltimateTool
./gradlew compileDebugKotlin compileDebugUnitTestKotlin --no-daemon --console=plain
rc=$?
echo "PHASE1_RC=$rc"
pkill -f "[G]radle" 2>/dev/null
exit $rc
