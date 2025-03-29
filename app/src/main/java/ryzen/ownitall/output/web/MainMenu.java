package ryzen.ownitall.output.web;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class MainMenu {

    public MainMenu(String[] args) {
    }

    @RequestMapping("/")
    String home() {
        return "Hello World!";
    }
}
