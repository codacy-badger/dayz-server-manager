package sk.dayz.modvalidator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages= {"sk.dayz.modvalidator"})
public class ModValidatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModValidatorApplication.class, args);
	}
}
