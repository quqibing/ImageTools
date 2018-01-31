package com.aicookie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FileUtils;
/**
 * 表把图片按照指定的大小分类到子目录中
 * @author qqb
 *
 */
public class ImageFilter {
	private static final String FORDER_PREFIX = "image-";
	/**
	 * 每多少条显示输出
	 */
	private static final int OUTPUT_NUM = 100;
	/**
	 * 最小宽高比
	 */
	private static final double MIN_AspectRatio = 0.25;

	/**
	 * 错误处理
	 */
	private static void error() {
		System.out.println("Too Few Argument.\njava ImageFilter c:/images 800(width) 600(height)");
	}

	/**
	 * 运行函数
	 * 
	 * @param args
	 *            处理图片 参数0是宽高比 参数1是文件尺寸数组 参数2是目录
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			error();
			return;
		}
		ImageFilter bot = new ImageFilter();
		bot.execute(args);
		System.out.println("--------\n");
	}

	/**
	 * 处理图片 参数0是宽高比 参数1是文件尺寸数组 参数2是目录
	 * 
	 * @param args
	 */
	private void execute(String[] args) {
		double ar = 0;
		try {
			ar = Double.valueOf(args[0]);
		} catch (Exception e) {
			ar = MIN_AspectRatio;
			e.printStackTrace();
		}
		// 读取文件目录
		String rootForder = args[2];
		rootForder = rootForder.replaceAll("\\\\", "/");
		if (!rootForder.endsWith("/"))
			rootForder = rootForder + "/";
		File forder = new File(rootForder);
		System.out.println(args[2]);
		File[] files = forder.listFiles();
		List<String> forders = new ArrayList<String>();
		forders.add(rootForder);
		addSubDir(files, forders);
		for (String str : forders)
			System.out.println("已添加目录:" + str);
		for (String fd : forders) {
			// 把GIF文件放入一个目录
			// ArrayList<String> lstGif = retrieveDirList(fd,
			// new String[] { "gif" });
			// if (lstGif != null & lstGif.size() != 0) {
			// // writeFile(fd + "gif.lst", lstGif);
			// moveLstFile(fd, lstGif, FORDER_PREFIX + "gif");
			// }

			// 得到图片分割数组信息
			String[] sizes = args[1].split(",");
			int[] squres = new int[sizes.length + 1]; // 把图片数组存成面积数组
			squres[0] = 0;
			for (int i = 0; i < sizes.length; i++) {
				String[] hw = sizes[i].split("\\*");
				sizes[i] = sizes[i].replaceAll("\\*", "_"); // 这个数组后面用于移动目录名
				int width = Integer.parseInt(hw[0]);
				int height = Integer.parseInt(hw[1]);
				squres[i + 1] = width * height; // 图片面积分割的数组

			}

			// 得到文件列表
			String[] extend = { "jpg", "jpeg", "bmp", "png", "gif" };
			ArrayList<String> lstFiles = retrieveDirList(fd, extend);

			// ************处理不同大小的图片***************
			if (lstFiles == null) {
				return;
			}
			int max = lstFiles.size();

			@SuppressWarnings("unchecked")
			ArrayList<String>[] lst = new ArrayList[squres.length]; // 不同面积文件列表
			ArrayList<String> arLst = new ArrayList<String>(); // ar文件列表
			for (int i = 0; i < lst.length; i++)
				lst[i] = new ArrayList<String>();

			// 开始处理图片列表
			System.out.println("开始处理" + fd + "的图片:");
			for (int i = 0; i < lstFiles.size(); i++) {
				String fn = lstFiles.get(i);
				Pic pic = getPictureInfo(fd, fn);
				if (pic == null)
					continue;
				String ext = fn.substring(fn.lastIndexOf(".") + 1);
				String format = pic.format;
				if (format.equalsIgnoreCase("jpeg"))
					format = "jpg";
				File imageFile = new File(fd, fn);
				if (!ext.equalsIgnoreCase(format)) {
					System.out.print("错误文件名:" + fn);
					// 格式不对重命名
					fn = fn.replace(ext, format.toLowerCase());
					System.out.println(",修改为:" + fn);
					File newFile = new File(fd + fn);
					imageFile.renameTo(newFile);
					imageFile = newFile;
				}
				if (format.equals("gif")) {
					// gif拷贝到gif里
					try {
						if (pic.height < 50 || pic.width < 50)
							FileUtils.moveFileToDirectory(imageFile, new File(fd + FORDER_PREFIX + "gif-small"), true);
						else
							FileUtils.moveFileToDirectory(imageFile, new File(fd + FORDER_PREFIX + "gif-large"), true);
					} catch (IOException e) {
						System.out.println(e.getMessage());
					}
					continue;
				}
				int qr = 0;
				// 如果获取不到图片，则处理下一张
				qr = pic.squre;
				boolean isInsqure = false;
				// 处理宽高比不合适的文件
				if (ar != 0 && checkAr(ar, pic.aspectRatio))
					arLst.add(fn);
				else {
					for (int j = 0; j < squres.length - 1; j++) {
						if (squres[j] <= qr && qr < squres[j + 1]) { // 在这个范围内,
							// 则进入这个列表
							isInsqure = true;
							lst[j].add(fn);
						}
					}
					// 不在这个范围内则是最大图片, 放在最后的数组里.
					if (!isInsqure)
						lst[lst.length - 1].add(fn);
				}
				if (i % OUTPUT_NUM == 0) {
					System.out.println(i + "/" + max);
				}
				if (i % 2000 == 0 && i != 0) {
					System.out.println("输出前" + i + "个图片.");
					output(fd, sizes, lst, arLst, i + "");
					arLst = new ArrayList<String>();
					for (int k = 0; k < lst.length; k++)
						lst[k] = new ArrayList<String>();
				}
			} // end for
			System.out.println(max + "/" + max);
			output(fd, sizes, lst, arLst, lstFiles.size() + "");
		}
		// 合并目录文件
		if (args.length == 4) {
			String isMerge = args[3];
			if (isMerge != null && isMerge.equals("merge") && forders.size() > 1) {
				int allFileNum = 0;
				int allErrorNum = 0;
				// 对于所有子目录
				for (int i = 1; i < forders.size(); i++) {
					File f = new File(forders.get(i));
					File[] subFiles = f.listFiles();
					// 对于所有子目录的子文件夹
					int fileNum = 0;
					int errorNum = 0;
					for (int j = 0; j < subFiles.length; j++) {
						File file = subFiles[j];
						if (file.isDirectory() && file.getName().indexOf(FORDER_PREFIX) >= 0) {
							File[] imgFiles = file.listFiles();
							File mainDir = new File(forders.get(0) + file.getName());
							// 对于每个文件
							for (int k = 0; k < imgFiles.length; k++) {
								if (imgFiles[k].isFile()) {
									try {
										FileUtils.moveFileToDirectory(imgFiles[k], mainDir, true);
										fileNum++;
									} catch (IOException e) {
										// System.out.println("合并移动:"+
										// imgFiles[k].getAbsolutePath()+ "出错:"
										// + e.getMessage());
										errorNum++;
									}
								}
							}
						}
					}
					System.out
							.println("已经合并目录:" + f.getAbsolutePath() + "中文件:" + fileNum + "个.未移动文件:" + errorNum + "个.");
					allFileNum += fileNum;
					allErrorNum += errorNum;
					fileNum = errorNum = 0;
				}
				System.out.println("总合并文件:" + allFileNum + "个.未移动文件:" + allErrorNum + "个.");
			}
		}

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
					addSubDir(file.listFiles(), forders);
				}

			}
		}
	}

	/**
	 * 输出图片文件
	 * 
	 * @param fd
	 * @param sizes
	 * @param lst
	 * @param arLst
	 */
	private void output(String fd, String[] sizes, ArrayList<String>[] lst, ArrayList<String> arLst, String index) {
		// 移动宽高比不合适图片
		// writeFile(fd + "ar"+ "_"+index+".lst", arLst);
		moveLstFile(fd, arLst, FORDER_PREFIX + "arNotNormal");

		// 移动最小图片
		String minFile = FORDER_PREFIX + sizes[0] + " - down";
		// writeFile(fd + minFile + "_"+index+".lst", lst[0]);
		moveLstFile(fd, lst[0], minFile);

		// 移动中间的图片
		for (int i = 1; i < lst.length - 1; i++) {
			// writeFile(fd + sizes[i - 1] + " - " + sizes[i] +
			// "_"+index+".lst", lst[i]);
			moveLstFile(fd, lst[i], FORDER_PREFIX + sizes[i - 1] + " - " + sizes[i]);
		}
		// 移动最大图片
		String maxFile = FORDER_PREFIX + sizes[sizes.length - 1] + " - up";
		// writeFile(fd + maxFile + "_"+index+".lst", lst[lst.length - 1]);
		moveLstFile(fd, lst[lst.length - 1], maxFile);
	}

	/**
	 * 如果输入宽高比大于1, 则提取大于宽高比的文件 如果输入宽高比小于1, 则提取小于宽高比的文件 如果输入宽高比等于1, 则提取等于宽高比的文件
	 * 
	 * @param ar
	 * @param lstFiles
	 * @param picInfo
	 */
	private boolean checkAr(double ar, double fileAr) {
		boolean isInAr = false;
		if (ar > 1 && fileAr > ar) {
			isInAr = true;
		}
		if (ar < 1 && fileAr < ar) {
			isInAr = true;
		}
		if (ar == 1 && fileAr == ar) {
			isInAr = true;
		}

		return isInAr;
	}

	/**
	 * 得到图片的信息 HEIGHT 高度 WIDHT 宽度 SQURE 面积 NAME 名字 AspectRatio 宽高比
	 * 
	 * @param fd
	 * @param fp
	 * @return
	 */
	private Pic getPictureInfo(String fd, String fp) {
		ImageInputStream iis = null;
		// System.out.println((new
		// ImageReaderWriterSpi()).getFormatNames());
		Pic pic = null;
		ImageReader reader = null;
		try {

			// 读取图片文件
			File imgFile = new File(fd + fp);
			iis = ImageIO.createImageInputStream(imgFile);
			/*
			 * 返回包含所有当前已注册 ImageReader 的 Iterator，这些 ImageReader 声称能够解码指定格式。
			 * 参数：formatName - 包含非正式格式名称 .（例如 "jpeg" 或 "tiff"）等 。
			 */
			Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
			if (!it.hasNext()) {
				// No readers found
				return null;
			}
			reader = it.next();
			/*
			 * <p>iis:读取源.true:只向前搜索 </p>.将它标记为 ‘只向前搜索’。
			 * 此设置意味着包含在输入源中的图像将只按顺序读取，可能允许 reader 避免缓存包含与以前已经读取的图像关联的数据的那些输入部分。
			 */
			reader.setInput(iis, true);

			int width = reader.getWidth(0);
			int height = reader.getHeight(0);
			pic = new Pic();
			pic.height = height;
			pic.width = width;
			pic.squre = width * height;
			pic.name = fp;
			pic.format = reader.getFormatName();
			pic.aspectRatio = (double) reader.getAspectRatio(0);
		} catch (Exception ex) {
			System.err.println(fp + ": " + ex.toString());
			return null;
		} finally {
			if (reader != null)
				reader.dispose();
			try {
				if (iis != null)
					iis.close();

			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
		}
		return pic;
	}

	class Pic {
		int height;
		int width;
		int squre;
		String name;
		String format;
		double aspectRatio;
	}

	/**
	 * 根据文件列表移动文件
	 * 
	 * @param fp
	 * @param dirname
	 */
	private void moveLstFile(String fd, List<String> lst, String dirname) {

		File dir = new File(fd + dirname);
		for (int i = 0; i < lst.size(); i++) {
			File file = new File(fd + lst.get(i));
			try {
				FileUtils.moveFileToDirectory(file, dir, true);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} finally {
			}
			// System.out.println("移动:"+line+" 到:"+dirname);
		}
		System.out.println(dirname + " 以内图片移动完成.");
	}

	/**
	 * 得到目录中图片列表, 不包含目录
	 * 
	 * @return
	 */
	private ArrayList<String> retrieveDirList(String fd, final String[] exts) {
		ArrayList<String> ary = new ArrayList<String>();
		String names[] = (new File(fd)).list();
		for (int j = 0; j < names.length; j++)
			for (int i = 0; i < exts.length; i++) {
				String ext = exts[i];
				if (names[j].toLowerCase().endsWith("." + ext))
					ary.add(names[j]);
			}
		return ary;
	}

	/**
	 * 把图片列表写入文件
	 * 
	 * @param string
	 * @param lstUn
	 */
	// private void writeFile(String string, List<String> lst) {
	// StringBuffer sb = new StringBuffer();
	// for (int i = 0; i < lst.size(); i++) {
	// sb.append(lst.get(i)).append("\r\n");
	// }
	// writeFile(string, sb.toString());
	// }

	/**
	 * 把内容写入文件把图片列表写入文件
	 * 
	 * @param fp
	 * @param content
	 */
	// private void writeFile(String fp, String content) {
	// FileWriter fw = null;
	// try {
	// fw = new FileWriter(fp);
	// fw.write(content);
	// } catch (IOException e) {
	// System.err.println("Write:" + fp + "failed:" + e.toString());
	// } finally {
	// try {
	// fw.close();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// }
	// }
}
