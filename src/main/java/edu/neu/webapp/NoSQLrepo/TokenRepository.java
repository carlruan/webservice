package edu.neu.webapp.NoSQLrepo;

import edu.neu.webapp.model.Token;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@EnableScan
@Repository
public interface TokenRepository extends CrudRepository<Token, String> {
    Token findByEmailAndToken(String email, String token);
    List<Token> findByEmail(String email);
}
