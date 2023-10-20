package backend.controller;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Topo {
    public List<topodatacenter> datacenters = null;
    public List<toponode> nodes = null;
    public List<JSONObject> links = null;
    public Topo() {
        this.datacenters = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.links = new ArrayList<>();
    }
}

class topodatacenter {
    public String name;
    public String type;
    public topodatacenter(String name, String type) {
        this.name = name;
        this.type = type;
    }
}

class toponode {
    public String name;
    public String type;
    public String datacenter;
    public int ports = 2;
    public long bw = 12500000;
    public long pes = 1;
    public long  mips = 30000000;
    public int ram = 10240;
    public long storage = 10000000;

    // switch
    public toponode(String name, String type, String datacenter, int ports, long bw){
        this.name = name;
        this.type = type;
        this.datacenter = datacenter;
        this.ports = ports;
        this.bw = bw;
    }

    // host
    public toponode(String name, String type, String datacenter, long bw){
        this.name = name;
        this.type = type;
        this.datacenter = datacenter;
        this.bw = bw;
    }

}
