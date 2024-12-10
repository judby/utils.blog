# Large File Splitter
Being able to split very large files into smaller to support uploads and similar.

Created mainly to support amazon S3 multipart file uploads, but implementation does not
depend on any Amazon services.

## Memory mapped files
We do not want to load huge files into java heap memory so will be using memory mapped files.

Issue with (the current JDK23 implementation) of MappedByteBuffer is that it only supports 
files up to 2G bytes in size (Integer.MAX_VALUE).

So this implementation splits the large file into multiple MappedByteBuffers
each as close to 2G in size as reasonable.

## Virtual threads
Since reading from files and writing to other media (files, sockets) are potentially blocking 
operations, these are great candidates for virtual threads.

The default split mechanism uses a virtual thread per part, potentially creating
hundredths of virtual threads.

## Using
...