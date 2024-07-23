# Release steps:

Replace 0.v.0 with your target release version:
```sh
cd java

mvn --batch-mode release:clean release:prepare -DreleaseVersion=0.v.0 -DskipTests -Darguments=-DskipTests
```
