# DuplicateFileFinder

Simple Java command line utility for finding duplicate files. It uses the java.nio.Files classes to traverse
directories and a corresponding Visitor to get details on these files. It checks for duplicates via file size
and if the file is the same size, then it performs an MD5 hash on the smaller of either the first 25% of the bytes of the file or the first 4k.

## Building
```java
./gradlew clean shadowJar
```

## Running
#### Finding Duplicate Files
```java
java -jar build/libs/dupe.jar -p ~/tmp/
```

#### Copying Duplicate Files
```java
java -jar build/libs/dupe.jar -v -p ~/tmp/ -c ~/tmp2/
```

#### Finding a Specific Duplicate File
```java
java -jar build/libs/dupe.jar -o ~/tmp/test-file -s ~/tmp
```