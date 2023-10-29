package backend.controller;

//import com.reins.bookstore.service.LoginService;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.main.SimpleExampleInterCloud;
import org.cloudbus.cloudsim.sdn.workload.Workload;
import org.cloudbus.cloudsim.sdn.workload.WorkloadResultWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RestController
//@Scope(value = "singleton")
@CrossOrigin
public class Controller {
    private SimpleExampleInterCloud simulator;

    private String physicalf = "InputOutput/physical2.xml";
    private String virtualf = "example-intercloud/intercloud.virtual2.json";
    private String workloadf = "example-intercloud/one-workload.csv";
    private boolean halfDuplex = false;

    @RequestMapping("/visit")
    public ResultDTO login(@RequestBody String req){
        System.out.println("访问后端");
        System.out.println(CloudSim.HalfDuplex);
        System.out.println(CloudSim.HalfDuplex);
        System.out.println(CloudSim.HalfDuplex);
        return ResultDTO.success("This is simulator backend");
    }

    @RequestMapping("/halfduplex")
    public ResultDTO halfduplex(@RequestBody String req) {
        JSONObject state = new JSONObject(req);
        halfDuplex = state.getBoolean("switchstate");
        System.out.println(String.valueOf(halfDuplex));
        CloudSim.HalfDuplex = halfDuplex;
        CloudSim.wirelessBw = 10000000; //10M
        return ResultDTO.success("ok");
    }

    @RequestMapping("/writeJsonFile")
    public String writeJsonFile() throws Exception {
        JSONObject obj1 = new JSONObject().put("name", "xxx").put("gender", "male").put("phone", "123");
        JSONObject obj2 = new JSONObject().put("name", "yyy").put("gender", "female").put("phone", "456");
        JSONArray array = new JSONArray();
        array.put(obj1).put(obj2);
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("./test.json"),"UTF-8");
        osw.write(array.toString());
        osw.flush();//清空缓冲区，强制输出数据
        osw.close();//关闭输出流
        return array.toString();
    }

    @RequestMapping("/readJsonFile")
    public String readJsonFile(String req) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("./test.json")));
        JSONArray array = new JSONArray(content);
        @SuppressWarnings("unchecked")
        Iterator<Object> iter = array.iterator();
        while(iter.hasNext()){
            System.out.println(iter.next());
        }
        return array.toString();
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
                .put("upports", 0)
                .put("downports", 0)
                .put("iops", 1000000000)
                .put("name","inter")
                .put("type","intercloud")
                .put("datacenter","net")
                .put("bw", 100000000); //100M
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
            long edgebw = dcConfig.getLong("edgebw") * 1000000; // MB
            long corebw = dcConfig.getLong("corebw") * 1000000; // MB
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
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name","gw"+String.valueOf(i))
                    .put("type","gateway")
                    .put("datacenter","net")
                    .put("bw", corebw);
            JSONObject gateway_copy = new JSONObject()
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name","gw"+String.valueOf(i))
                    .put("type","gateway")
                    .put("datacenter","dc"+String.valueOf(i))
                    .put("bw", corebw);
            topo.accumulate("nodes", gateway).accumulate("nodes",gateway_copy);
            // 解析dc的core switches
            for(int j=1; j<=corenum; ++j){
                JSONObject core = new JSONObject()
                        .put("upports", 0)
                        .put("downports", 0)
                        .put("iops", 1000000000)
                        .put("name",String.valueOf(i)+"core"+String.valueOf(j))
                        .put("type","core")
                        .put("datacenter","dc"+String.valueOf(i))
                        .put("ports",coreports)
                        .put("bw", corebw);
                topo.accumulate("nodes", core);
                //新建link: gw <-> core
                topo.accumulate("links", new JSONObject().put("source","gw"+String.valueOf(i))
                        .put("destination", String.valueOf(i)+"core"+String.valueOf(j)).put("latency", 1.0));
            }
            // 解析dc的edge switches
            for(int j=1; j<=edgenum; ++j){
                JSONObject edge = new JSONObject()
                        .put("upports", 0)
                        .put("downports", 0)
                        .put("iops", 1000000000)
                        .put("name",String.valueOf(i)+"edge"+String.valueOf(j))
                        .put("type","edge")
                        .put("datacenter","dc"+String.valueOf(i))
                        .put("ports",edgeports)
                        .put("bw", edgebw);
                topo.accumulate("nodes", edge);
                //新建link: core <-> edge. 比如edge1~2连core1
                topo.accumulate("links", new JSONObject().put("source",String.valueOf(i)+"core"+String.valueOf((j-1+coreports)/coreports))
                        .put("destination", String.valueOf(i)+"edge"+String.valueOf(j)).put("latency", 1.0));
            }
            // 解析dc的hosts
            for(int j=1; j<=hostnum; ++j){
                JSONObject host = new JSONObject()
                        .put("upports", 0)
                        .put("downports", 0)
                        .put("iops", 1000000000)
                        .put("name",String.valueOf(i)+"host"+String.valueOf(j))
                        .put("type","host")
                        .put("datacenter","dc"+String.valueOf(i))
                        .put("bw", edgebw)
                        .put("pes",1)
                        .put("mips",30000000)
                        .put("ram", 10240)
                        .put("storage", 10000000);
                topo.accumulate("nodes", host);
                //新建link: edge <-> host. 比如host1~2连edge1
                topo.accumulate("links", new JSONObject().put("source",String.valueOf(i)+"edge"+String.valueOf((j-1+edgeports)/edgeports))
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
        String args[] = {"",physicalf,virtualf,workloadf};
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
