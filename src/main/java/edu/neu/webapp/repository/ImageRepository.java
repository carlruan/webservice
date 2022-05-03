package edu.neu.webapp.repository;

import edu.neu.webapp.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {
    Image findByUserId(String userid);
    void deleteByUserId(String userid);
}
