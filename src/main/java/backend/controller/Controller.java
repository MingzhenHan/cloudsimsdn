package backend.controller;

//import com.reins.bookstore.service.LoginService;
import org.cloudbus.cloudsim.sdn.main.SimpleExampleInterCloud;
import org.cloudbus.cloudsim.sdn.workload.Workload;
import org.cloudbus.cloudsim.sdn.workload.WorkloadResultWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    @RequestMapping("/createtopo")
    public ResultDTO createTopo(/*@RequestBody Map<String, String> req*/){
        System.out.println("创建物理拓扑");
//        JSONObject topo = new JSONObject();
//        List<JSONObject> datacenters = new ArrayList<JSONObject>();
//        for(int i=0; i<3; ++i){//比如3个dc
//            JSONObject dc = new JSONObject().put("name","dc"+String.valueOf(i)).put("type", "cloud");
////            dc.put("name", "dc"+String.valueOf(i));
////            dc.put("type", "cloud");
//            datacenters.add(dc);
//        }
//        JSONObject[] dcs = datacenters.toArray(new JSONObject[datacenters.size()]);
//        topo.put("datacenters", 1);
//        topo.put("nodes", 1);
//        topo.put("links",1);
//        return ResultDTO.success(topo.toString());
        Topo topo = new Topo();
        List<topodatacenter> datacenters = new ArrayList<>();
        for(int i=0; i<3; ++i){//比如3个dc
            topodatacenter dc = new topodatacenter("dc"+String.valueOf(i), "cloud");
            datacenters.add(dc);
        }
        List<toponode> nodes = new ArrayList<>();
        for(int i=0; i<3; ++i){//比如3个dc
            toponode node = new toponode("host"+String.valueOf(i), "host", "dc1", 1000000);
            nodes.add(node);
            node = new toponode("edge"+String.valueOf(i), "edge", "dc1", 2,1000000);
            nodes.add(node);
        }
        topo.datacenters = datacenters;
        topo.nodes = nodes;

        return ResultDTO.success(topo);
    }

    @RequestMapping("/writephysical")
    public ResultDTO JsonWrite(@RequestBody String req) throws Exception{
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("InputOutput/exampleWrite.json"),"UTF-8");
        JSONObject topo = new JSONObject();
        JSONArray array= new JSONArray(req);
        System.out.println(array);
        // 解析wirelessnetwork
        topo.accumulate("datacenters", new JSONObject().put("name","net").put("type", "wirelessnetwork"));
        // 解析wirelessnetworkd的interswitch
        JSONObject inter = new JSONObject()
                .put("name","inter")
                .put("type","interclouud")
                .put("datacenter","net")
                .put("bw", 10000000);
        topo.accumulate("nodes", inter);

        for(int i=1;i<=array.length();i++)
        {
            //新建linklink: inter <-> gw
            topo.accumulate("links", new JSONObject().put("source","inter")
                    .put("destination", "gw"+String.valueOf(i)).put("latency", 1.0));
            JSONObject dcConfig = array.getJSONObject(i-1);
            //-------------以下是dcConfig
            int hostnum = dcConfig.getInt("hostnum");//4;
            int edgeports = dcConfig.getInt("edgeports");//2;
            int coreports = dcConfig.getInt("coreports");//2;
            //-------------
            int edgenum = (hostnum+edgeports-1) / edgeports;
            int corenum = (edgenum+coreports-1) / coreports;
            // 解析dc
            JSONObject dc = new JSONObject()
                    .put("name","dc"+String.valueOf(i))
                    .put("type", "cloud");
            topo.accumulate("datacenters", dc);
            // 解析dc的gateway
            JSONObject gateway = new JSONObject()
                    .put("name","gw"+String.valueOf(i))
                    .put("type","gateway")
                    .put("datacenter","net")
                    .put("bw", 10000000);
            JSONObject gateway_copy = new JSONObject()
                    .put("name","gw"+String.valueOf(i))
                    .put("type","gateway")
                    .put("datacenter","dc"+String.valueOf(i))
                    .put("bw", 10000000);
            topo.accumulate("nodes", gateway).accumulate("nodes",gateway_copy);
            // 解析dc的core switches
            for(int j=1; j<=corenum; ++j){
                JSONObject core = new JSONObject()
                        .put("name",String.valueOf(i)+"core"+String.valueOf(j))
                        .put("type","core")
                        .put("datacenter","dc"+String.valueOf(i))
                        .put("ports",coreports)
                        .put("bw", 10000000);
                topo.accumulate("nodes", core);
                //新建link: gw <-> core
                topo.accumulate("links", new JSONObject().put("source","gw"+String.valueOf(i))
                        .put("destination", String.valueOf(i)+"core"+String.valueOf(j)).put("latency", 1.0));
            }
            // 解析dc的edge switches
            for(int j=1; j<=edgenum; ++j){
                JSONObject edge = new JSONObject()
                        .put("name",String.valueOf(i)+"edge"+String.valueOf(j))
                        .put("type","edge")
                        .put("datacenter","dc"+String.valueOf(i))
                        .put("ports",edgeports)
                        .put("bw", 10000000);
                topo.accumulate("nodes", edge);
                //新建link: core <-> edge. 比如edge1~2连core1
                topo.accumulate("links", new JSONObject().put("source",String.valueOf(i)+"core"+String.valueOf((j-1+corenum)/corenum))
                        .put("destination", String.valueOf(i)+"edge"+String.valueOf(j)).put("latency", 1.0));
            }
            // 解析dc的hosts
            for(int j=1; j<=hostnum; ++j){
                JSONObject host = new JSONObject()
                        .put("name",String.valueOf(i)+"host"+String.valueOf(j))
                        .put("type","host")
                        .put("datacenter","dc"+String.valueOf(i))
                        .put("bw", 10000000)
                        .put("pes",1)
                        .put("mips",30000000)
                        .put("ram", 10240)
                        .put("storage", 10000000);
                topo.accumulate("nodes", host);
                //新建link: edge <-> host. 比如host1~2连edge1
                topo.accumulate("links", new JSONObject().put("source",String.valueOf(i)+"edge"+String.valueOf((j-1+corenum)/corenum))
                        .put("destination", String.valueOf(i)+"host"+String.valueOf(j)).put("latency", 1.0));
            }
        }
        osw.write(topo.toString());
        osw.flush();//清空缓冲区，强制输出数据
        osw.close();//关闭输出流
        return ResultDTO.success(topo.toString());
    }

    @PostMapping("/uploadphysical")
    public ResultDTO uploadPhysical(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传物理拓扑文件");
        try {
            String InputOutput = System.getProperty("user.dir")+"\\InputOutput";
            System.out.println(InputOutput);
            file.transferTo(new File(InputOutput, file.getOriginalFilename() ));
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
            String InputOutput = System.getProperty("user.dir")+"\\InputOutput";
            System.out.println(InputOutput);
            file.transferTo(new File(InputOutput, file.getOriginalFilename() ));
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
            String InputOutput = System.getProperty("user.dir")+"\\InputOutput";
            System.out.println(InputOutput);
            file.transferTo(new File(InputOutput, file.getOriginalFilename() ));
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
            wr.jobid = workload.jobId;
            wr.workloadid = workload.workloadId;
            wr.vmid = workload.submitVmName;
            wr.destid = workload.destVmName;
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
