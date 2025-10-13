package guimemorygame.model;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class Leaderboard {
	
	public void createLeaderboard() {
		/*
		 * corrects the leaderboard.txt file to the right format if empty
		 */
		try {
			BufferedReader rd  = new BufferedReader(new FileReader("./Resources/leaderboard.txt"));
			String line = rd.readLine();
			
			if(line == null) {
				BufferedWriter wr  = new BufferedWriter(new FileWriter("./Resources/leaderboard.txt"));
				wr.write("Easy: ");
				wr.newLine();
				wr.write("Normal: ");
				wr.newLine();
				wr.write("Hard: ");
				wr.close();
			}
			rd.close();
		}
		catch (Exception e) {
			System.out.println("File not found");
		}
	}
	
	public String displayLeaderboard() {
		/*
		 * displays the leaderboard.txt file contents to the player
		 */
		String display = "";
		try {
			BufferedReader rd  = new BufferedReader(new FileReader("./Resources/leaderboard.txt"));
			
			String line = rd.readLine();
				for (int i = 1; i <= 3; i ++) {
					display += line + "<br>";
					line = rd.readLine();
				}
			
			rd.close();
		}
		
		catch (Exception e) {
				System.out.println("File not found");
		}
		return display;
	}
	
	public void updateLeaderboard(String name, String difficulty, int turn) {		
		/* 
		 * reads the leaderboard.txt file and checks if the most recent
		 * played game has beaten any of the records set and updates the file
		 * if necessary
		 */
		try {
			BufferedReader rd  = new BufferedReader(new FileReader("./Resources/leaderboard.txt"));
			
			String line1 = rd.readLine();
			String[] words1 = line1.split(" ");
			String line2 = rd.readLine();
			String[] words2 = line2.split(" ");
			String line3 = rd.readLine();
			String[] words3 = line3.split(" ");
			
			BufferedWriter wr  = new BufferedWriter(new FileWriter("./Resources/leaderboard.txt"));

			if (words1.length != 3) {
				if(difficulty.equalsIgnoreCase("Easy")) {
					wr.write("Easy: " + name + ", " + turn);
					wr.newLine();
				}
				else {
					wr.write(line1);
					wr.newLine();
				}
			}
			else {
				if(Integer.parseInt(words1[2]) > turn) {
					wr.write("Easy: " + name + ", " + turn);
					wr.newLine();
				}
				else {
					wr.write(line1);
					wr.newLine();
				}
			}
			
			if (words2.length != 3) {
				if(difficulty.equalsIgnoreCase("Normal")) {
					wr.write("Normal: " + name + ", " + turn);
					wr.newLine();
				}
				else {
					wr.write(line2);
					wr.newLine();
				}

			}
			else {
				if(Integer.parseInt(words2[2]) > turn) {
					wr.write("Normal: " + name + ", " + turn);
					wr.newLine();
				}
				else {
					wr.write(line2);
					wr.newLine();
				}
			}
			
			if (words3.length != 3) {
				if(difficulty.equalsIgnoreCase("Hard")) {
					wr.write("Hard: " + name + ", " + turn);
					wr.newLine();
				}
				else {
					wr.write(line3);
					wr.newLine();
				}
			}
			else {
				if(Integer.parseInt(words3[2]) > turn) {
					wr.write("Hard: " + name + ", " + turn);
					wr.newLine();
				}
				else {
					wr.write(line3);
					wr.newLine();
				}
			}
				
			rd.close();
			wr.close();
		}		
		catch (Exception e) {
			System.out.println("File not found");
		}
	}
}
