package com.paulograbin;

public record FileRecord(String name,
                         long length,
                         String group,
                         boolean tombstone,
                         String dateString,
                         String creationTime,
                         String lastModifiedTime,
                         String lastAccessTime) {
}
