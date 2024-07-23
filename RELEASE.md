# Release steps:
```sh
cd java

mvn --batch-mode release:clean release:prepare -DreleaseVersion=0.v.0 -DskipTests -Darguments=-DskipTests
```