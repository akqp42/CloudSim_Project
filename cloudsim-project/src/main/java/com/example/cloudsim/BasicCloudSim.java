package com.example.cloudsim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.*;

public class BasicCloudSim {
    public static void main(String[] args) {
        int numUsers = 1;
        Calendar calendar = Calendar.getInstance();
        boolean traceFlag = false;
        CloudSim.init(numUsers, calendar, traceFlag);

        // Create Datacenter
        Datacenter datacenter = createDatacenter("Datacenter_1");

        // Create Broker
        DatacenterBroker broker = createBroker();
        int brokerId = broker.getId();

        // Create VMs
        List<Vm> vmList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Vm vm = new Vm(i, brokerId, 1000, 1, 512, 1000, 10000,
                    "Xen", new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }
        broker.submitVmList(vmList);

        // Create Cloudlets
        List<Cloudlet> cloudletList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Cloudlet cloudlet = new Cloudlet(i, 40000, 1, 300, 300,
                    new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }
        broker.submitCloudletList(cloudletList);

        // Auto-scaling: Add more VMs if workload is high
        if (cloudletList.size() > vmList.size() * 2) {
            for (int i = 2; i < 4; i++) {
                Vm vm = new Vm(i, brokerId, 1000, 1, 512, 1000, 10000,
                        "Xen", new CloudletSchedulerTimeShared());
                vmList.add(vm);
            }
            broker.submitVmList(vmList);
            System.out.println("Auto-scaling: Added 2 more VMs based on workload size.");
        }

        // Start simulation
        CloudSim.startSimulation();
        List<Cloudlet> newList = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        // Print results
        printCloudletList(newList);
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
    
        // Create 2 Hosts with more resources
        for (int hostId = 0; hostId < 2; hostId++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(1000))); // Increased MIPS per PE
            peList.add(new Pe(1, new PeProvisionerSimple(1000))); // Increased MIPS per PE
    
            int ram = 4096; // Increase RAM to handle more VMs
            long storage = 1000000;
            int bw = 10000;
    
            Host host = new Host(
                    hostId,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList)
            );
            hostList.add(host);
        }
    
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double timeZone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.1;
        double costPerBw = 0.1;
    
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw
        );
    
        Datacenter datacenter = null;
        try {
            // Use VmAllocationPolicySimple as available in CloudSim 3.0.3
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<Storage>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }     
       

    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return broker;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent +
                "Datacenter ID" + indent + "VM ID" + indent + "Time" + indent +
                "Start Time" + indent + "Finish Time");

        for (Cloudlet cloudlet : list) {
            System.out.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                System.out.printf("SUCCESS%s%d%s%d%s%.2f%s%.2f%s%.2f\n",
                        indent, cloudlet.getResourceId(),
                        indent, cloudlet.getVmId(),
                        indent, cloudlet.getActualCPUTime(),
                        indent, cloudlet.getExecStartTime(),
                        indent, cloudlet.getFinishTime());
            }
        }
    }
}