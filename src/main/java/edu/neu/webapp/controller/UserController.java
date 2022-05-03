package edu.neu.webapp.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.timgroup.statsd.StatsDClient;
import edu.neu.webapp.exception.RequestException;
import edu.neu.webapp.model.Image;
import edu.neu.webapp.model.Token;
import edu.neu.webapp.model.User;
import edu.neu.webapp.repository.ImageRepository;
import edu.neu.webapp.NoSQLrepo.TokenRepository;
import edu.neu.webapp.repository.UserRepository;
import edu.neu.webapp.service.S3BucketStorageService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.naming.NameNotFoundException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class UserController {

    @Autowired
    private AmazonS3 amazonS3Client;

    @Autowired
    private AmazonSNS amazonSNS;

    @Value("${bucketName}")
    private String bucketName;

    @Value("${topicArn}")
    private String topicArn;

    @Autowired
    S3BucketStorageService s3service;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepo;

    @Autowired
    ImageRepository imageRepo;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    private StatsDClient statsDClient;

    @GetMapping("/hello")
    public String init() {
        return "hello world";
    }

    @PostMapping("/user")
    public ResponseEntity<User> createUser(@Validated @RequestBody User user) {

        statsDClient.incrementCounter("endpoint.v1.http.post.user");
        if (!EmailValidator.getInstance().isValid(user.getUsername())) {
            throw new RequestException("Email is invalid!");
        }
        if (userRepo.findByUsername(user.getUsername()) != null) {
            if(userRepo.findByUsername(user.getUsername()).getVerified()){
                throw new RequestException("User already exists!");
            }else{
                long cur = System.currentTimeMillis() / 1000L;
                for(Token tmp : tokenRepository.findByEmail(user.getUsername())){
                    if(cur - tmp.getTtl() < 0){
                        throw new RequestException("Valid email has been sent!");
                    }
                }
                //user = userRepo.findByUsername(user.getUsername());
            }
        }
        if(userRepo.findByUsername(user.getUsername()) != null){
            User org = userRepo.findByUsername(user.getUsername());
            org.setPassword(passwordEncoder.encode(user.getPassword()));
            org.setFirst_name(user.getFirst_name());
            org.setLast_name(user.getLast_name());
            user = org;
        }else user.setPassword(passwordEncoder.encode(user.getPassword()));
        String tk = user.getId() + System.currentTimeMillis();
        String msg = user.getUsername() + ":" + tk;
        amazonSNS.publish(new PublishRequest().withTopicArn(topicArn).withMessage(msg));
        Token token = new Token();
        token.setTtl(System.currentTimeMillis() / 1000L + 2 * 60);
        token.setEmail(user.getUsername());
        token.setToken(tk);
        tokenRepository.save(token);
        return ResponseEntity.created(null).body(userRepo.save(user));
    }

    @GetMapping("/verifyUserEmail")
    public ResponseEntity<User> validateUser(@RequestParam(value = "email") String email, @RequestParam(value = "token") String token){
        if(email == null || email.equals("") || token == null || token.equals("")) throw new RequestException("Url not valid!");
        long cur = System.currentTimeMillis() / 1000L;
        Token tk = tokenRepository.findByEmailAndToken(email, token);
        if(tk == null || cur > tk.getTtl()) throw new RequestException("Token not valid!");
        User user = userRepo.findByUsername(email);
        if(user == null) throw new RequestException("User not found!");
        user.setVerified(true);
        userRepo.save(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/self")
    public ResponseEntity<User> getUsers(@RequestHeader(value = "Authorization", required = false) String header){
        statsDClient.incrementCounter("endpoint.v1.http.get.user");
        if (header == null) {
            throw new RequestException("Unauthorized!");
        }
        String username = null;
        String token = null;
        String pass = null;
        if (header != null && header.startsWith("Basic ")) {
            //jwtToken = header.substring(7);
            token = header.substring(6);
            if (token.length() < 5) throw new RequestException("Unauthorized!");
            String decodedString = new String(Base64.decodeBase64(token.getBytes()));
            String[] temp = decodedString.split(":");
            username = temp[0];

            if (temp.length == 1) throw new RequestException("Unauthorized!");
            pass = temp[1];

            //username = jwtUtility.getUsernameFromToken(jwtToken);
        }

        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new RequestException("User not found!");
        }
        if(!user.getVerified()){
            throw new RequestException("User not valid!");
        }
        if (passwordEncoder.matches(pass, user.getPassword())) {
            return ResponseEntity.ok().body(user);
        } else {
            throw new RequestException("Password wrong!");
        }

    }

    @PutMapping("/user/self")
    public ResponseEntity updateUser(@Validated @RequestBody User user, @RequestHeader(value = "Authorization", required = false) String header){
        statsDClient.incrementCounter("endpoint.v1.http.put.user.self");
        if (header == null) {
            throw new RequestException("Unauthorized!");
        }
        String username = null;
        //String jwtToken = null;
        String token = null;
        String pass = null;
        if (header != null && header.startsWith("Basic ")) {
            //jwtToken = header.substring(7);
            token = header.substring(6);
            if (token.length() < 5) throw new RequestException("Unauthorized!");
            //username = jwtUtility.getUsernameFromToken(jwtToken);
            String decodedString = new String(Base64.decodeBase64(token.getBytes()));
            String[] temp = decodedString.split(":");
            username = temp[0];
            if (temp.length == 1) throw new RequestException("Unauthorized!");
            pass = temp[1];

        }
        User origin = userRepo.findByUsername(username);
        if (origin == null) {
            throw new RequestException("User not found!");
        }
        if(!origin.getVerified()){
            throw new RequestException("User not valid!");
        }
        if (passwordEncoder.matches(pass, origin.getPassword())) {
            if (!user.getUsername().equals(origin.getUsername())
                    || user.getAccount_created() != null
                    || user.getAccount_updated() != null) {
                throw new RequestException("You cannot update certain fields!");
            }
            if (user.getPassword() != null) {
                origin.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            if (user.getFirst_name() != null) {
                origin.setFirst_name(user.getFirst_name());
            }
            if (user.getLast_name() != null) {
                origin.setLast_name(user.getLast_name());
            }

            userRepo.save(origin);
            return ResponseEntity.noContent().build();
        } else {
            throw new RequestException("Password wrong!");
        }
    }

    @PostMapping("/user/self/pic")
    @Transactional
    public ResponseEntity postPic(@RequestHeader(value = "Authorization", required = false) String header,
                                  @RequestParam MultipartFile file) {
        statsDClient.incrementCounter("endpoint.v1.http.post.user.self.pic");
        if (header == null) {
            throw new RequestException("Unauthorized!");
        }
        String username = null;
        String token = null;
        String pass = null;
        if (header.startsWith("Basic ")) {
            //jwtToken = header.substring(7);
            token = header.substring(6);
            if (token.length() < 5) throw new RequestException("Unauthorized!");
            String decodedString = new String(Base64.decodeBase64(token.getBytes()));
            String[] temp = decodedString.split(":");
            username = temp[0];
            if (temp.length == 1) throw new RequestException("Unauthorized!");
            pass = temp[1];
            //username = jwtUtility.getUsernameFromToken(jwtToken);
        }

        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new RequestException("User not found!");
        }
        if(!user.getVerified()){
            throw new RequestException("User not valid!");
        }
        if (passwordEncoder.matches(pass, user.getPassword())) {
            String filename = file.getOriginalFilename();
            String[] fn = filename.split("\\.");
            String suffix = fn[fn.length - 1];
            if(!suffix.equalsIgnoreCase("jpg") && !suffix.equalsIgnoreCase("png") && !suffix.equalsIgnoreCase("jpeg")){
                throw new RequestException("File error!");
            }
            if(imageRepo.findByUserId(user.getId()) != null) {
                String tempFile = imageRepo.findByUserId(user.getId()).getFile_name();
                imageRepo.deleteByUserId(user.getId());
                s3service.deleteFile(user.getId() + "/" + tempFile);
            }
            s3service.uploadFile(user.getId() + "/" + file.getOriginalFilename(), file);
            Image image = new Image();
            image.setUserId(user.getId());
            image.setFile_name(file.getOriginalFilename());
            image.setUrl(amazonS3Client.getUrl(bucketName, user.getId() + "/" + file.getOriginalFilename()).toString());
            image.setUpload_date(amazonS3Client.getObject(bucketName, user.getId() + "/" + file.getOriginalFilename())
                    .getObjectMetadata()
                    .getLastModified());
            return  ResponseEntity.created(null).body(imageRepo.save(image));
            //return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            throw new RequestException("Password wrong!");
        }
    }

    @GetMapping("/user/self/pic")
    public ResponseEntity getPic(@RequestHeader(value = "Authorization", required = false) String header) {
        statsDClient.incrementCounter("endpoint.v1.http.get.user.self.pic");
        if (header == null) {
            throw new RequestException("Unauthorized!");
        }
        String username = null;
        String token = null;
        String pass = null;
        if (header != null && header.startsWith("Basic ")) {
            //jwtToken = header.substring(7);
            token = header.substring(6);
            if (token.length() < 5) throw new RequestException("Unauthorized!");
            String decodedString = new String(Base64.decodeBase64(token.getBytes()));
            String[] temp = decodedString.split(":");
            username = temp[0];

            if (temp.length == 1) throw new RequestException("Unauthorized!");
            pass = temp[1];

            //username = jwtUtility.getUsernameFromToken(jwtToken);
        }

        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new RequestException("User not found!");
        }
        if(!user.getVerified()){
            throw new RequestException("User not valid!");
        }
        if (passwordEncoder.matches(pass, user.getPassword())) {
            Image image = imageRepo.findByUserId(user.getId());
            if(image != null){
                return ResponseEntity.ok().body(image);
            }else{
                return ResponseEntity.notFound().build();
            }

        } else {
            throw new RequestException("Password wrong!");
        }
    }

    @DeleteMapping("/user/self/pic")
    @Transactional
    public ResponseEntity deletePic(@RequestHeader(value = "Authorization", required = false) String header) {
        statsDClient.incrementCounter("endpoint.v1.http.delete.user.self.pic");
        if (header == null) {
            throw new RequestException("Unauthorized!");
        }
        String username = null;
        String token;
        String pass = null;
        if (header.startsWith("Basic ")) {
            //jwtToken = header.substring(7);
            token = header.substring(6);
            if (token.length() < 5) throw new RequestException("Unauthorized!");
            String decodedString = new String(Base64.decodeBase64(token.getBytes()));
            String[] temp = decodedString.split(":");
            username = temp[0];

            if (temp.length == 1) throw new RequestException("Unauthorized!");
            pass = temp[1];

            //username = jwtUtility.getUsernameFromToken(jwtToken);
        }

        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new RequestException("User not found!");
        }
        if(!user.getVerified()){
            throw new RequestException("User not valid!");
        }
        if (passwordEncoder.matches(pass, user.getPassword())) {
            if(imageRepo.findByUserId(user.getId()) != null){

                String tempFile = imageRepo.findByUserId(user.getId()).getFile_name();
                imageRepo.deleteByUserId(user.getId());
                s3service.deleteFile(user.getId() + "/" + tempFile);
                return ResponseEntity.noContent().build();
            }else{
                return ResponseEntity.notFound().build();
            }


        } else {
            throw new RequestException("Password wrong!");
        }
    }

}
