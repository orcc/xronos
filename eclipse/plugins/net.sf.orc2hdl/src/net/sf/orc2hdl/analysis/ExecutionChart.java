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

				ArrayList<Integer> previousGoDone = timeGoDone.get(0);
				Integer start = 0;
				Integer stop = 0;
				for (int i = 0; i < timeGoDone.size(); i++) {
					Integer timeBy100 = i * 100;
					ArrayList<Integer> goDone = timeGoDone.get(timeBy100);

					Integer state = actionState(previousGoDone, goDone);

					switch (state) {
					case 0:
						break;
					case 1:
						stop = timeBy100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
						break;
					case 2:
						start = timeBy100;
						break;
					case 3:
						start = timeBy100;
						break;
					case 4:
						stop = timeBy100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
						break;
					case 5:
						stop = timeBy100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
						break;
					case 6:
						stop = timeBy100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
						break;
					case 7:
						stop = timeBy100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
						start = stop;
						break;
					case 8:
						stop = timeBy100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
						break;
					case 9:
						stop = timeBy100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
						start = stop;
						break;
					case 10:
						stop = timeBy100;
						actionExecution.put(idx, Arrays.asList(start, stop));
						idx++;
						start = stop;
						break;
					default:
						break;
					}

					previousGoDone = goDone;

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
		final JFreeChart chart = ChartFactory.createGanttChart("Network:"
				+ network.getSimpleName() + " HW Execution", // chart
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

	private Integer goDoneState(ArrayList<Integer> goDone) {
		Integer state = 0;

		if (goDone.get(0).equals(0) && goDone.get(1).equals(0)) {
			state = 0;
		} else if (goDone.get(0).equals(0) && goDone.get(1).equals(1)) {
			state = 1;
		} else if (goDone.get(0).equals(1) && goDone.get(1).equals(0)) {
			state = 2;
		} else if (goDone.get(0).equals(1) && goDone.get(1).equals(1)) {
			state = 3;
		}
		return state;
	}

	private Integer actionState(ArrayList<Integer> previousGoDone,
			ArrayList<Integer> goDone) {
		Integer state = 0;

		Integer previousState = goDoneState(previousGoDone);
		Integer currentState = goDoneState(goDone);

		if (previousState == 0 && currentState == 0) {
			state = 0;
		} else if (previousState == 0 && currentState == 1) {
			state = 1;
		} else if (previousState == 0 && currentState == 2) {
			state = 2;
		} else if (previousState == 0 && currentState == 3) {
			state = 3;
		} else if (previousState == 1 && currentState == 0) {
			state = 4;
		} else if (previousState == 2 && currentState == 0) {
			state = 5;
		} else if (previousState == 2 && currentState == 1) {
			state = 6;
		} else if (previousState == 2 && currentState == 3) {
			state = 7;
		} else if (previousState == 3 && currentState == 0) {
			state = 8;
		} else if (previousState == 3 && currentState == 1) {
			state = 9;
		} else if (previousState == 3 && currentState == 3) {
			state = 10;
		}

		return state;

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
