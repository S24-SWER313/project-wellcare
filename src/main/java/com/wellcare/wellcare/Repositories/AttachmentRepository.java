package com.wellcare.wellcare.Repositories;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wellcare.wellcare.Models.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}
