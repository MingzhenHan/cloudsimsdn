package backend.controller;

//import com.reins.bookstore.service.LoginService;
import org.cloudbus.cloudsim.sdn.example.SimpleExampleInterCloud;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@Scope(value = "singleton")
@CrossOrigin
public class Controller {
    private SimpleExampleInterCloud simulator = new SimpleExampleInterCloud();

    @RequestMapping("/visit")
    public ResultDTO login(@RequestBody Map<String, String> req){
        System.out.println("访问后端");
        return ResultDTO.success("This is simulator backend");
    }

    @RequestMapping("/run")
    public ResultDTO run(@RequestBody Map<String, String> req) throws IOException {
        System.out.println("\n开始仿真");
        String args[] = {"LFF","example-intercloud/intercloud.physical2.xml","example-intercloud/intercloud.virtual2.json", "example-intercloud/one-workload.csv"};
        simulator.main(args);
        return ResultDTO.success("Simulation completed");
    }
}
