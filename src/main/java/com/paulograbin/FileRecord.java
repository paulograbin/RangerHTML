package com.paulograbin;

public record FileRecord(String name,
                         long length,
                         String group,
                         boolean tombstone,
                         String creationDate,
                         String start,
                         String end,
                         java.time.LocalDateTime creationTime) {
}
