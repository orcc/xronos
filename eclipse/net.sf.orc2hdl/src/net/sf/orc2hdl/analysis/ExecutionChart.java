package net.sf.orc2hdl.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.orcc.df.Action;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;

public class ExecutionChart {

	private Network network;
	private Map<Instance, Map<Action, TimeGoDone>> execution;
	private String path;
	JFreeChart chart;

	public ExecutionChart(Map<Instance, Map<Action, TimeGoDone>> execution,
			Network network, String path) {
		this.execution = execution;
		this.network = network;
		this.path = path;
		IntervalCategoryDataset dataset = createDataset();
		chart = createChart(dataset, network);
	}

	public IntervalCategoryDataset createDataset() {
		TaskSeries taskSeries = new TaskSeries("Action Execution");
		TaskSeriesCollection collection = new TaskSeriesCollection();

		for (Instance instance : network.getInstances()) {
			Map<Action, TimeGoDone> actionsGoDone = execution.get(instance);
			for (Action action : instance.getActor().getActions()) {
				TimeGoDone timeGoDone = actionsGoDone.get(action);
				Map<Integer, List<Integer>> actionExecution = new TreeMap<Integer, List<Integer>>();
				int idx = 0;
				for (int i = 0; i < timeGoDone.size(); i++) {

					ArrayList<Integer> goDone = timeGoDone.get(i * 100);
					Integer start = 0;
					Integer stop = 0;
					Boolean minGoSpacing = false;

					if ((goDone.get(0).equals(1) && goDone.get(1).equals(0))) {
						start = i * 100;
						minGoSpacing = true;
					} else if ((goDone.get(0).equals(1) && goDone.get(1)
							.equals(1))) {
						start = i * 100;
						stop = i * 100;
						if (minGoSpacing) {
							start = i;
							minGoSpacing = false;
						}
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;

					} else if ((goDone.get(0).equals(0) && goDone.get(1)
							.equals(1))) {
						stop = i * 100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
					}

				}
				if (!actionExecution.isEmpty()) {
					long str = (long) actionExecution.get(0).get(0);
					long stp = (long) actionExecution.get(0).get(1);

					Task task = new Task(instance.getSimpleName() + "_"
							+ action.getName(), new SimpleTimePeriod(new Date(
							str), new Date(stp)));

					for (int j = 1; j < actionExecution.size(); j++) {
						str = (long) actionExecution.get(j).get(0);
						stp = (long) actionExecution.get(j).get(1);

						task.addSubtask(new Task(instance.getSimpleName() + "_"
								+ action.getName(), new SimpleTimePeriod(
								new Date(str), new Date(stp))));

					}
					taskSeries.add(task);
				}
			}

		}
		collection.add(taskSeries);
		return collection;
	}

	/**
	 * Creates a Gantt chart based on input data set
	 */
	private JFreeChart createChart(IntervalCategoryDataset dataset,
			Network network) {
		final JFreeChart chart = ChartFactory.createGanttChart(
				"Network:" + network.getSimpleName()+ " HW Execution", // chart
												// title
				"Actions", // domain axis label
				"Time(ns)", // range axis label
				dataset, // data
				true, // include legend
				true, // tooltips
				false // urls
				);
		return chart;

	}

	public void saveChart() {
		try {
			/**
			 * This utility saves the JFreeChart as a JPEG First Parameter:
			 * FileName Second Parameter: Chart To Save Third Parameter: Height
			 * Of Picture Fourth Parameter: Width Of Picture
			 */
			ChartUtilities.saveChartAsJPEG(new File(path + File.separator
					+ network.getSimpleName() + "_executionTrace.jpg"), chart,
					1920, 1080);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Problem occurred creating chart.");
		}
	}
}
