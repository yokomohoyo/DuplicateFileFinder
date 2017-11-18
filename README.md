# DuplicateFileFinder

Simple Java command line utility for finding duplicate files. It uses the java.nio.Files classes to traverse
directories and a corresponding Visitor to get details on these files. It checks for duplicates via file size
and if the file is the same size, then it performs an MD5 hash on the first 10% of the bytes of the file.

##Building
```java
./gradlew clean shadowJar
```

##Running
```java
java -jar build/libs/dupe.jar -p ~/tmp/
```