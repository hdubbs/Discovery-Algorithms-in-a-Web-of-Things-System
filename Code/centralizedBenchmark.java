package com.siemens.ct.discovery;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import com.siemens.ct.urdf.Client;

/**
 * A simple benchmark used to test the times required for a centralized discovery. This benchmark ignores the time it takes for a NodeMCU to load its uRDF-store.
 * @author Hannah Wills
 */

public class CentralizedBenchmark {

	private static String ipPrefix = "coap://192.168.";

	private static CsvFileWriter benchmarkFile;

	public static void main(String[] args) {
		List<String> ipAdresses = new ArrayList<String>();
		
		ipAdresses.add(ipPrefix + "2.140"); // actuators (NodeMCU 3)
		ipAdresses.add(ipPrefix + "2.179"); //pmag (NodeMCU 2)
		ipAdresses.add(ipPrefix + "2.166"); //lut400 (NodeMCU 1)
		
		benchmark (ipAdresses, 5);
	}

	/**
	 * Runs the benchmark and records the time it takes for selected functions
	 * to run. The Data is recorded in a csv file with the format <FunctionName, ModelID, Time(ns)>
	 * @param ips
	 *            a list of IP Adresses that are in the network, which are to be
	 *            benchmarked.
	 * @param benchmarkID
	 *            The number of the benchmark, should be updated for each benchmark so old trials are not overwritten
	 */
	private static void benchmark(List<String> ips, int benchmarkID) {
		CentralizedDiscovery cd = new CentralizedDiscovery();
		List<String> benchmarkData = new ArrayList<String>();
		long startTime, endTime;

		for (String ip : ips) {
			// Wait for user to "add" new NodeMCU to network
			System.out.println("Waiting for user to trigger query [press enter]: ");
			Scanner scanner = new Scanner(System.in);
			scanner.nextLine();
			

			startTime = System.nanoTime();
			int i = cd.discover(ip);
			endTime = System.nanoTime();
			 System.out.println("Not necessary for benchmarks. \nTime taken for Discovery in ns: " + (endTime - startTime));
			benchmarkData.add("discovery," + i + "," + (endTime - startTime));
			for (int j = 0; j <= i; j++) {
				startTime = System.nanoTime();
				cd.update(j);
				endTime = System.nanoTime();

				 System.out.println("Model " + i + " takes : " + (endTime - startTime));
				benchmarkData.add("update," + j + "," + (endTime - startTime));
			}
		}
		String filename = "Benchmark_321_" + benchmarkID + ".csv";
		CsvFileWriter.writeCsvFile(filename, benchmarkData);

	}

	/**
	 * Preparatory for the benchmark, runtime here is not recorded. Loads
	 * uRDF-store on remote Servient.
	 * 
	 * @param ipAdresses
	 * @return
	 */
	private List<Client> prepareClients(String[] ipAdresses) {
		List<Client> clients = new ArrayList<Client>();
		Stack<Model> models = createModels(); // assumes num ofIPs = num of models

		for (String ipAdress : ipAdresses) {
			Client c = new Client(ipAdress);
			c.addAll(models.pop());
			clients.add(c);
		}
		return clients;
	}

	/**
	 * Builds models from semantic from turtle files of the three nodemcus from
	 * the FESTO water management unit.
	 * @return A Stack of Models, which represent the semantic data of the FESTO unit
	 */
	private Stack<Model> createModels() {
		Stack<Model> models = new Stack<Model>();

		models.add(createSampleModel("actuators.ttl"));
		models.add(createSampleModel("lut400.ttl"));
		models.add(createSampleModel("pmag.ttl"));

		return models;
	}

	private static Model createSampleModel(String filename) {
		Model m = ModelFactory.createDefaultModel();

		URL res = ClassLoader.getSystemClassLoader().getResource(filename);
		m.read(res.toString());

		return m;
	}
}