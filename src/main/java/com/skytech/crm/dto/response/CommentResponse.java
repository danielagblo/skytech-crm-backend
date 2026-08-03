package com.skytech.crm.dto.response;

import java.time.*;
import java.util.*;

public record CommentResponse(
    UUID id,
    UUID parentCommentId,
    UUID authorId,
    String authorName,
    String body,
    OffsetDateTime createdAt) {}
