#!/usr/bin/env bash
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
if ! command -v mvn >/dev/null 2>&1; then
  export M2_HOME="$HOME/tools/apache-maven-3.9.12"
  export PATH="$M2_HOME/bin:$JAVA_HOME/bin:$PATH"
fi
mvn -DskipTests org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=com.accounting.ui.MainApplication
