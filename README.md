# DuplicateFileFinder
Simple Java command line utility for finding duplicate files. It traverses directories recursively to hunt for duplicate 
files. It first matches by size and then uses a hash off the tail end of the file.

## Building
```java
./gradlew clean shadowJar // To run as jar only
./gradlew clean assembleDist && 
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
java -jar build/libs/dupe.jar -n ~/tmp/test-file -h ~/tmp
```