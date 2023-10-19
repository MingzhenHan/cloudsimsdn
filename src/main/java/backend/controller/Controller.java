package backend.controller;

//import com.reins.bookstore.service.LoginService;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.example.SimpleExampleInterCloud;
import org.cloudbus.cloudsim.sdn.workload.Workload;
import org.cloudbus.cloudsim.sdn.workload.WorkloadResultWriter;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
//@Scope(value = "singleton")
@CrossOrigin
public class Controller {
    private SimpleExampleInterCloud simulator;

    private String physicalf = "InputOutput/physical2.xml";
    private String virtualf = "InputOutput/virtual2.json";
    private String workloadf = "InputOutput/one-workload.csv";
    @RequestMapping("/visit")
    public ResultDTO login(@RequestBody Map<String, String> req){
        System.out.println("访问后端");
        return ResultDTO.success("This is simulator backend");
    }

    @PostMapping("/uploadphysical")
    public ResultDTO uploadPhysical(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传物理拓扑文件");
        try {
            file.transferTo(new File("D:\\11桌面备份11\\CloudSim-djy\\FAB\\cloudsimsdn\\InputOutput", file.getOriginalFilename() ));
            physicalf = "InputOutput/" + file.getOriginalFilename();
        }catch (IOException e){
            System.out.print(e.getMessage());
        }
        return ResultDTO.success("上传成功");
    }

    @PostMapping("/uploadvirtual")
    public ResultDTO uploadVirtual(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传虚拟拓扑文件");
        try {
            file.transferTo(new File("D:\\11桌面备份11\\CloudSim-djy\\FAB\\cloudsimsdn\\InputOutput", file.getOriginalFilename() ));
            virtualf = "InputOutput/" + file.getOriginalFilename();
        }catch (IOException e){
            System.out.print(e.getMessage());
        }
        return ResultDTO.success("上传成功");
    }

    @PostMapping("/uploadworkload")
    public ResultDTO uploadWorkload(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传负载文件");
        try {
            file.transferTo(new File("D:\\11桌面备份11\\CloudSim-djy\\FAB\\cloudsimsdn\\InputOutput", file.getOriginalFilename() ));
            workloadf = "InputOutput/" + file.getOriginalFilename();
        }catch (IOException e){
            System.out.print(e.getMessage());
        }
        return ResultDTO.success("上传成功");
    }

    @RequestMapping("/run")
    public ResultDTO run() throws IOException {
        System.out.println("\n开始仿真");
//        String args[] = {"LFF","example-intercloud/intercloud.physical2.xml","example-intercloud/intercloud.virtual2.json", "example-intercloud/one-workload.csv"};
        String args[] = {"LFF",physicalf,virtualf,workloadf};
        simulator = new SimpleExampleInterCloud();
        List<Workload> wls = simulator.main(args);
        List<WorkloadResult> wrlist = new ArrayList<>();
        for(Workload workload:wls){
//------------------------------------------ calculate total time
            double finishTime = -1;
            double startTime = -1;
            double Time = -1;
            finishTime = WorkloadResultWriter.getWorkloadFinishTime(workload);
            startTime = WorkloadResultWriter.getWorkloadStartTime(workload);
            if (finishTime > 0)
                Time = finishTime - startTime;
//------------------------------------------
            WorkloadResult wr = new WorkloadResult();
            wr.jobid = workload.workloadId;
            wr.vmid = workload.submitVmName;
            if(workload.failed)
                wr.status = "timeout";
            else
                wr.status = "arrived";
            wr.finishtime = String.format("%.4f", finishTime);
            wr.starttime = String.format("%.4f", startTime);
            wr.time = String.format("%.4f", Time);
            wrlist.add(wr);
        }
        WorkloadResult[] wrarray = wrlist.toArray(new WorkloadResult[wrlist.size()]);
        return ResultDTO.success(wrarray);
    }
}
