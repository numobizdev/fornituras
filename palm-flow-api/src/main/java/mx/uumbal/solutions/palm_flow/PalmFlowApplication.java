package mx.uumbal.solutions.palm_flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PalmFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(PalmFlowApplication.class, args);
	}

}
