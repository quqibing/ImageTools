package com.aicookie;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

/**
 * 按照一个目录中的文件,删除或移动另一个目录中多出来的文件
 * 
 * @author 曲奇饼
 *
 */
public class SyncDeleteFiles {

	private static Set<String> imgTypes = new HashSet<String>();

	static {
		imgTypes.add("jpg");
		imgTypes.add("jpeg");
		imgTypes.add("png");
		imgTypes.add("gif");
		imgTypes.add("bmp");
		imgTypes.add("ico");
	}

	/**
	 * 错误处理
	 */
	private static void error() {
		System.out.println("例子:java SyncDeleteFiles c:/images c:/images1 -moveD:/moveToe");
		System.out.println("参数0是源目录绝对路径 ");
		System.out.println("参数1是目标处理目录绝对路径.");
		System.out.println("参数2是模式: -delete,删除;-move+目录名表示移动到的目录.");
	}

	/**
	 * 运行函数
	 * 
	 * @param args
	 *            见execute的说明
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			error();
			return;
		}
		SyncDeleteFiles sdf = new SyncDeleteFiles();
		sdf.execute(args[0], args[1], args[2]);
	}

	/**
	 * 处理目录
	 * 
	 * @param srcDir
	 *            源目录绝对路径
	 * @param dealDir
	 *            目标处理目录绝对路径
	 * @param mode
	 *            模式: -delete,删除;-move+目录名表示移动到的目录.
	 */
	private void execute(String srcDir, String dealDir, String mode) {
		if (mode == null || (mode.indexOf("-delete") < 0 && mode.indexOf("-move") < 0)) {
			System.out.println("模式错误!");
			error();
			return;
		}
		File srcForder = new File(srcDir);
		File dealForder = new File(dealDir);
		File[] files = srcForder.listFiles();
		if (files == null || files.length == 0) {
			System.out.println("源目录中没有文件.");
			return;
		}
		File[] dealFiles = dealForder.listFiles();
		if (dealFiles == null || dealFiles.length == 0) {
			System.out.println("目标目录中没有文件.");
			return;
		}
		Set<String> fileNames = new HashSet<String>();
		for (File file : files) {
			if (file.isFile())
				fileNames.add(file.getName());
		}
		File moveFolder = null;
		if (mode.startsWith("-move"))
			moveFolder = new File(mode.substring(5));
		for (File file : dealFiles) {
			if (file.isDirectory() || fileNames.contains(file.getName()))
				continue;
			else if (file.getName().indexOf(".xml") < 0) {
				if ("-delete".equals(mode)) {
					file.delete();
					System.out.println("已删除文件:" + file.getName());
				} else if (moveFolder != null) {
					try {
						FileUtils.moveFileToDirectory(file, moveFolder, true);
					} catch (IOException e) {
						System.out.println("处理文件出错:" + file.getAbsolutePath() + ",错误:" + e.getMessage());
					}
					System.out.println("已移动文件:" + file.getName());
				}
			}
		}
		System.out.println("全部处理完成!");
	}

}
