package org.isfce.pid.dao;

import java.util.List;

import org.isfce.pid.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDocumentDao extends JpaRepository<Document, Long> {

    List<Document> findByDossierId(Long dossierId);
}
