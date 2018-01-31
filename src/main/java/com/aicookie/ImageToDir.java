package com.aicookie;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.io.FileUtils;

/**
 * 把图片按照指定的数量放到子目录里
 * 
 * @author qqb
 *
 */
public class ImageToDir {
	private static String FORDER_PREFIX = "img";

	/**
	 * 错误处理
	 */
	private static void error() {
		System.out.println("参数:" + "\n例子: java ImageToDir 20 c:/images wallpaper dyyMMdd\n" + "参数0是每个目录的文件数量 \n"
				+ "参数1是目录.\n" + "参数2是输出目录前缀.\n" + "参数3是后缀模式, dyyMMdd表示日期后缀,模式为yyMMdd, n%03d表示数字后缀,模式为%03d");
	}

	/**
	 * 运行函数
	 * 
	 * @param args
	 *            见execute的说明
	 */
	public static void main(String[] args) {
		if (args.length < 4) {
			error();
			return;
		}
		ImageToDir bot = new ImageToDir();
		int fileNumber = 0;
		try {
			fileNumber = Integer.valueOf(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		bot.execute(fileNumber, args[1], args[2], args[3]);
		System.out.println("--------\n");
	}

	/**
	 * 处理图片
	 * 
	 * @param fileNumber
	 *            目录文件数
	 * @param rootForder
	 *            根目录
	 * @param forderPrefix
	 *            目录前缀
	 * @param sufixMode
	 *            后缀模式, dyyMMdd表示日期后缀,模式为yyMMdd, n%03d表示数字后缀,模式为%03d
	 */
	private void execute(Integer fileNumber, String rootForder, String forderPrefix, String sufixMode) {

		if (forderPrefix != null)
			FORDER_PREFIX = forderPrefix;
		System.out.println("目录前缀:" + FORDER_PREFIX);
		String type = sufixMode.substring(0, 0);
		SimpleDateFormat sdf = null;
		String numberMode = "%02d";
		if ("d".equals(type)) {
			sdf = new SimpleDateFormat("yyMMdd");
		} else if ("n".equals(type)) {
			numberMode = sufixMode.substring(1);
		}

		Calendar c = Calendar.getInstance();

		// 读取文件目录
		rootForder = rootForder.replaceAll("\\\\", "/");
		if (!rootForder.endsWith("/"))
			rootForder = rootForder + "/";
		File forder = new File(rootForder);
		System.out.println("根路径:" + rootForder);
		File[] files = forder.listFiles();
		if (files == null) {
			System.out.println("没有要处理的图片.");
			return;
		}
		List<String> forders = new ArrayList<String>();
		forders.add(rootForder);
		addSubDir(files, forders);
		for (String str : forders)
			System.out.println("已添加目录:" + str);
		ArrayList<String> lstFiles = new ArrayList<String>();
		for (String fd : forders) {

			// 得到文件列表
			String[] extend = { "jpg", "jpeg", "bmp", "png", "gif" };
			lstFiles.addAll(retrieveDirList(fd, extend));
		}
		String currentForder = rootForder;
		String currentForderName = null;
		int index = 1;
		int folderNumber = 1;
		// 开始处理图片列表
		System.out.println("开始处理图片:");
		for (int i = 0; i < lstFiles.size(); i++) {
			String fn = lstFiles.get(i);

			if (i % fileNumber == 0) {
				index = 1;
				if (sdf != null)
					currentForderName = FORDER_PREFIX + sdf.format(c.getTime());
				else
					currentForderName = FORDER_PREFIX + String.format(numberMode, folderNumber);
				currentForder = rootForder + currentForderName + "/";

				System.out.println("目录：" + currentForder);
				c.add(Calendar.DAY_OF_YEAR, 1);
				folderNumber++;
			}
			File sFile = new File(fn);
			String fileExt = fn.substring(fn.lastIndexOf("."));
			File destFile = new File(currentForder + currentForderName + "_" + String.format("%03d", index) + fileExt);
			try {
				FileUtils.moveFile(sFile, destFile);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} finally {
			}
			index++;
		} // end for

	}

	/**
	 * 递归添加非生成的子目录
	 * 
	 * @param files
	 * @param forders
	 */
	public void addSubDir(File[] files, List<String> forders) {
		for (File file : files) {
			if (file.isDirectory()) {
				String absolutePath = file.getAbsolutePath();
				// 排除已经处理过生成的目录
				if (absolutePath.indexOf(FORDER_PREFIX) < 0) {
					forders.add(absolutePath + "/");
					if (file.listFiles() != null)
						addSubDir(file.listFiles(), forders);
				}

			}
		}
	}

	/**
	 * 得到目录中图片列表, 不包含目录
	 * 
	 * @return
	 */
	private ArrayList<String> retrieveDirList(String fd, final String[] exts) {
		ArrayList<String> ary = new ArrayList<String>();
		fd.replaceAll("\\\\", "/");
		if (!fd.endsWith("/"))
			fd += "/";
		String names[] = (new File(fd)).list();
		for (int j = 0; j < names.length; j++)
			for (int i = 0; i < exts.length; i++) {
				String ext = exts[i];
				if (names[j].toLowerCase().endsWith("." + ext))
					ary.add(fd + names[j]);
			}
		return ary;
	}

}
