package ch.bbw.rs.fileupload152.repository;

import ch.bbw.rs.fileupload152.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {

}