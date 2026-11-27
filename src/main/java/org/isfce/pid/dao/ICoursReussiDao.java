package org.isfce.pid.dao;

import java.util.List;

import org.isfce.pid.model.CoursReussi;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICoursReussiDao extends JpaRepository<CoursReussi, Long> {

    List<CoursReussi> findByDossierId(Long dossierId);
}
