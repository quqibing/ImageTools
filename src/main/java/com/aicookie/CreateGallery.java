package com.aicookie;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.sanselan.formats.jpeg.iptc.IPTCConstants;
import org.apache.sanselan.formats.jpeg.iptc.IPTCRecord;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;
import marvin.plugin.Crop;

/**
 * 产生Piwigo Gallery需要的图片格式
 * 
 * @author 曲奇饼
 * 
 */
public class CreateGallery {
	private static final String PWG_THUMB = "thumbnail";
	private static final String PWG_HIGH = "pwg_high";
	// 普通壁纸尺寸
	private int normalWidth = 1920;
	private int normalHeight = 1080;
	private float jpgQuality = 0.7f;

	// 缩略图尺寸
	private int thumbWidth = 120;
	private int thumbHeight = 96;

	private int beginIndex = 1;

	private String filePrefix = null;

	private String THUMB_PREFIX = "TN-";

	public static final int CUT_MODE_TOP = 0;
	public static final int CUT_MODE_MIDDLE = -20000;
	public static final int CUT_MODE_BUTTOM = -30000;
	public static final int CUT_MODE_LEFT = 0;
	public static final int CUT_MODE_CENTER = -20000;
	public static final int CUT_MODE_RIGHT = -30000;
	public static final String MODE_HIGH = "HIGH";
	public static final String MODE_NORMAL = "NORMAL";
	public static final String MODE_NORMAL_CUT = "NORMAL_CUT";
	private static final String MODE_HIGH_NOT_CUT = "HIGH_NOT_CUT";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args == null || args.length < 3) {
			System.out.println("请输入参数:");
			System.out.println("1.源目录");
			System.out.println("2.输出目录:");
			System.out.println(
					"3.模式:\n\tHIGH模式是生成HIGH目录, \n\tNORMAL模式不生成HIGH目录,并且不对图进行剪裁."
			+ "\n\tNORMAL_CUT模式不生成HIGH目录,对图进行剪裁."
			+ "\n\tHIGH_NOT_CUT生成指定尺寸比较小图片但保留原图");
			System.out.println("可选参数:");
			System.out.println("-width= 目标宽度, 默认 1920");
			System.out.println("-height= 目标高度, 默认 1080");
			System.out.println("-thumbWidth= 缩略图宽度, 默认 120");
			System.out.println("-thumbHeight= 缩略图高度, 默认 96");
			System.out.println("-jpgQuality= jpg质量, 默认 0.7f");
			System.out.println("-index= 开始序号, 默认 1");
			System.out.println("-prefix= 命名前缀, 前缀可选, 不填写前缀则用父目录名作为前缀.");
			return;
		}
		CreateGallery cg = new CreateGallery();
		try {
			cg.setNormalWidth(Integer.valueOf(getArgValue(args, "-width")));
		} catch (Exception e) {
		}

		try {
			cg.setNormalHeight(Integer.valueOf(getArgValue(args, "-height")));
		} catch (Exception e) {
		}

		try {
			cg.setThumbWidth(Integer.valueOf(getArgValue(args, "-thumbWidth")));
		} catch (Exception e) {
		}

		try {
			cg.setThumbHeight(Integer.valueOf(getArgValue(args, "-thumbHeight")));
		} catch (Exception e) {
		}

		try {
			cg.setJpgQuality(Float.valueOf(getArgValue(args, "-jpgQuality")));
		} catch (Exception e) {
		}

		try {
			cg.setBeginIndex(Integer.valueOf(getArgValue(args, "-beginIndex")));
		} catch (Exception e) {
		}
		try {
			cg.setFilePrefix(getArgValue(args, "-prefix"));
		} catch (Exception e) {
		}
		cg.execute(args[0], args[1], args[2]);

	}

	/**
	 * 获取可选参数的值.
	 * 
	 * @param args
	 * @param prefix
	 * @return
	 */
	private static String getArgValue(String[] args, String prefix) {
		if (args == null || args.length == 0 || prefix == null)
			return null;
		for (String arg : args) {
			if (arg.indexOf(prefix + "=") >= 0) {
				// 有"=", 所以+1
				return arg.substring(prefix.length() + 1);
			}
		}
		return null;
	}

	/**
	 * 处理图片
	 * 
	 * @param srcFolderName
	 *            源目录
	 * @param disFolderName
	 *            输出目录
	 * @param mode
	 *            模式
	 */
	public void execute(String srcFolderName, String disFolderName, String mode) { // 读取文件源目录
		Map<String,String> namesMapping = new HashMap<String,String>();
		srcFolderName = getFormatFolderName(srcFolderName);

		// 读取文件目标目录
		disFolderName = getFormatFolderName(disFolderName);

		File srcFolder = new File(srcFolderName);
		File disFolder = new File(disFolderName);
		if (!disFolder.exists())
			disFolder.mkdirs();

		if (this.filePrefix == null)
			this.filePrefix = disFolder.getName();

		// 得到文件列表
		String[] extend = { "jpg", "jpeg", "bmp", "png" };
		String names[] = srcFolder.list();// 读取文件列表
		// 创建高分辨率目录
		File hdpiDir = new File(disFolderName + "pwg_high/");
		if (!hdpiDir.exists() && (mode.equals(MODE_HIGH) || mode.equals(MODE_HIGH_NOT_CUT)))
			hdpiDir.mkdirs();

		// 创建低分辨率目录
		File ldpiDir = new File(disFolderName + PWG_THUMB);
		if (!ldpiDir.exists())
			ldpiDir.mkdirs();

		// 读取metaInfo信息
		List<IPTCRecord> commonMetaInfo = readIPTCMetaFile(srcFolderName + "metaInfo.xml");
		int index = beginIndex;// 图片索引

		for (int j = 0; j < names.length; j++)
			for (int i = 0; i < extend.length; i++) {
				String ext = extend[i];
				if (!names[j].toLowerCase().endsWith("." + ext))
					continue; // 不在扩展名列表里的文件跳过
				try {
					String srcImgPath = srcFolderName + names[j];
					String timeNow = (new Date()).getTime() + "";
					String disNameSubfix = filePrefix + String.format("%05d", index) + "-"
							+ timeNow.substring(timeNow.length() - 8) + ".jpg";

					String largeImgPath = disFolderName + PWG_HIGH + "/" + disNameSubfix;
					String smallImgPath = disFolderName + PWG_THUMB + "/" + THUMB_PREFIX + disNameSubfix;
					String normalImgPath = disFolderName + disNameSubfix;
					namesMapping.put(srcImgPath,normalImgPath);
					// 获取文件名中的keyword, 并且不能修改commonMetaInfo,所以要用newMetaInfo
					List<IPTCRecord> newMetaInfo = getFileNameKeys(srcImgPath);
					newMetaInfo.addAll(commonMetaInfo);
					Iptc iptc = new Iptc();
					if (mode.equals(MODE_HIGH)) {
						convertIamgeToJpg(srcImgPath, largeImgPath);
						iptc.updateIptc(largeImgPath, newMetaInfo);

						resizeCropImage(largeImgPath, normalImgPath, normalWidth, normalHeight, CUT_MODE_CENTER,
								CUT_MODE_TOP);
						iptc.updateIptc(normalImgPath, newMetaInfo);

						resizeCropImage(largeImgPath, smallImgPath, thumbWidth, thumbHeight, CUT_MODE_CENTER,
								CUT_MODE_TOP);
						iptc.updateIptc(smallImgPath, newMetaInfo);
					} else if (mode.equals(MODE_NORMAL)) {
						convertIamgeToJpg(srcImgPath, normalImgPath);
						iptc.updateIptc(normalImgPath, newMetaInfo);

						resizeCropImage(normalImgPath, smallImgPath, thumbWidth, thumbHeight, CUT_MODE_CENTER,
								CUT_MODE_TOP);
						iptc.updateIptc(smallImgPath, newMetaInfo);

					} else if (mode.equals(MODE_NORMAL_CUT)) {
						resizeCropImage(srcImgPath, normalImgPath, normalWidth, normalHeight, CUT_MODE_CENTER,
								CUT_MODE_TOP);
						iptc.updateIptc(normalImgPath, newMetaInfo);

						resizeCropImage(normalImgPath, smallImgPath, thumbWidth, thumbHeight, CUT_MODE_CENTER,
								CUT_MODE_TOP);
						iptc.updateIptc(smallImgPath, newMetaInfo);

					} else if (mode.equals(MODE_HIGH_NOT_CUT)) {
						jpgQuality = 0.8f;
						convertIamgeToJpg(srcImgPath, largeImgPath);
						iptc.updateIptc(largeImgPath, newMetaInfo);

						resizeImage(largeImgPath, normalImgPath, normalWidth, normalHeight, false);
						iptc.updateIptc(normalImgPath, newMetaInfo);

						resizeCropImage(largeImgPath, smallImgPath, thumbWidth, thumbHeight, CUT_MODE_CENTER,
								CUT_MODE_TOP);
						iptc.updateIptc(smallImgPath, newMetaInfo);

					} else {
						logOutput("不符合任何一种剪裁模式! HIGH模式是生成HIGH目录, 然后缩放并剪裁图片;"
								+ " NORMAL模式不生成高清目录,并且不对图进行剪裁;  HIGH_NOT_CUT缩放到指定尺寸图片(较小的不放大)但不剪裁, 原图放在高清目录里.");
						return;
					}

				} catch (Exception e) {
					e.printStackTrace();
					System.gc();
					logOutput("错误:" + names[j]);
					index--;// 失败的话,序号不长.
				}
				index++;
			}
		File mappingFile = new File(srcFolder+"/name-mapping.txt");
		//System.out.println("mappingFile.exists():"+mappingFile.exists());
		if(mappingFile.exists()){
			try {
				String oldMapping = FileUtils.readFileToString(mappingFile,"UTF-8");
				if(oldMapping != null && !oldMapping.trim().equals("")){
					for(String map : oldMapping.split("\n")){
						String[] m = map.split(",");
						if(m[0] !=null && !m[0].trim().equals("") && !namesMapping.containsKey(m[0]))
							namesMapping.put(m[0], m[1]);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		StringBuilder namesMappingSbu = new StringBuilder();
		for(String key : namesMapping.keySet())
			namesMappingSbu.append(key).append(",").append(namesMapping.get(key)).append("\n");
		try {
			FileUtils.writeStringToFile(mappingFile, namesMappingSbu.toString(), "UTF-8", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		logOutput("全部处理完毕!");

	}

	/**
	 * 格式化目录路径
	 * 
	 * @param folderName
	 * @return
	 */
	private String getFormatFolderName(String folderName) {
		String formatted = folderName.replaceAll("\\\\", "/");
		if (!formatted.endsWith("/"))
			formatted = formatted + "/";
		return formatted;
	}

	/**
	 * 读取IPTC信息XML文件, 可以读取的标签有name,description和key
	 * 
	 * @param xmlPath
	 * @return
	 * @throws MetaXmlNotFoundException
	 */
	public List<IPTCRecord> readIPTCMetaFile(String xmlPath) {
		ArrayList<IPTCRecord> newMeta = new ArrayList<IPTCRecord>();
		// （1）得到DOM解析器的工厂实例
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		// 得到javax.xml.parsers.DocumentBuilderFactory;类的实例就是我们要的解析器工厂
		InputStream is = null;
		try {
			// （2）从DOM工厂获得DOM解析器
			DocumentBuilder dombuilder = domfac.newDocumentBuilder();
			// 通过javax.xml.parsers.DocumentBuilderFactory实例的静态方法newDocumentBuilder()得到DOM解析器
			// （3）把要解析的XML文档转化为输入流，以便DOM解析器解析它
			is = new FileInputStream(xmlPath);
			// （4）解析XML文档的输入流，得到一个Document
			Document doc = dombuilder.parse(is);
			// 由XML文档的输入流得到一个org.w3c.dom.Document对象，以后的处理都是对Document对象进行的
			// （5）得到XML文档的根节点
			// Element root = doc.getDocumentElement();
			// 在DOM中只有根节点是一个org.w3c.dom.Element对象。

			newMeta.add(new IPTCRecord(IPTCConstants.IPTC_TYPE_OBJECT_NAME, new String(
					doc.getElementsByTagName("name").item(0).getTextContent().trim().getBytes(), "ISO-8859-1")));
			newMeta.add(new IPTCRecord(IPTCConstants.IPTC_TYPE_CAPTION_ABSTRACT, new String(
					doc.getElementsByTagName("description").item(0).getTextContent().trim().getBytes(), "ISO-8859-1")));
			NodeList keys = doc.getElementsByTagName("key");
			for (int i = 0; i < keys.getLength(); i++) {
				newMeta.add(new IPTCRecord(IPTCConstants.IPTC_TYPE_KEYWORDS,
						new String(keys.item(i).getTextContent().getBytes(), "ISO-8859-1")));
			}

		} catch (Exception e) {
			logOutput("未找到Meta的XML文件:" + e.getMessage());
			throw new RuntimeException("未找到Meta的XML文件", e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return newMeta;
	}

	/**
	 * 通过文件名生成IPTC keywords, 会把原来关键字删除 只有以p开头含有"_"并且以" "分隔的文件会读取key
	 * 
	 * @param imgPath
	 * @param newMeta
	 * @return 返回一个新的IPTCEntryCollection, 不修改传入的.
	 */
	private List<IPTCRecord> getFileNameKeys(String imgPath) {
		List<IPTCRecord> meta = new ArrayList<IPTCRecord>();
		// 获取主文件名
		String fileName = imgPath.substring(0, imgPath.lastIndexOf("."));
		if (imgPath.contains("/"))
			fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
		// 根据我所需要的文件名格式取得tags
		if (fileName.startsWith("p") && fileName.contains(" ") && fileName.contains("_")) {
			String[] tags = fileName.substring(fileName.indexOf("_") + 1).split(" ");
			for (String tag : tags) {
				try {
					if (tag.endsWith("_cr"))
						tag = tag.substring(0, tag.indexOf("_cr"));
					meta.add(
							new IPTCRecord(IPTCConstants.IPTC_TYPE_KEYWORDS, new String(tag.getBytes(), "ISO-8859-1")));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		if (fileName.startsWith("yande") && fileName.contains(" ")) {
			String[] tags = fileName.split(" ");
			for (int i=2; i < tags.length;i++) {
				String tag = tags[i];
				try {
					meta.add(
							new IPTCRecord(IPTCConstants.IPTC_TYPE_KEYWORDS, new String(tag.getBytes(), "ISO-8859-1")));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		return meta;
	}

	/**
	 * 把图片转换成JPG
	 * 
	 * @param srcPath
	 * @param disPath
	 * @param jpgQuality
	 * @throws IOException
	 */
	private void convertIamgeToJpg(String srcPath, String disPath) throws IOException {
		// 装载图像
		FileUtils.copyFile(new File(srcPath), new File(disPath));
		BufferedImage image = ImageIO.read(new File(disPath));
		
		saveImage(image, disPath);

	}

	private void saveImage(BufferedImage image, String disPath) throws IOException {
//		ImageWriter writer = null;
//		ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
//		Iterator<ImageWriter> iter = ImageIO.getImageWriters(type, "jpg");
//		if (iter.hasNext()) {
//			writer = (ImageWriter) iter.next();
//		}
//		if (writer == null) {
//			return;
//		}
//		IIOImage iioImage = new IIOImage(image, null, null);
//		ImageWriteParam param = writer.getDefaultWriteParam();
//		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//		param.setCompressionQuality(jpgQuality);
//		ImageOutputStream outputStream = ImageIO.createImageOutputStream(new File(disPath));
//		writer.setOutput(outputStream);
//		writer.write(null, iioImage, param);
//		outputStream.close();
//		writer.dispose();// 一定要销毁,要不后面程序访问不了这个文件.
		image.flush();
		if (!ImageIO.write(image, "JPEG", new File(disPath)))  
            System.out.println("保存文件:"+disPath+"失败.");;  
	}

	/**
	 * 按照要求缩放并剪裁图片
	 * 
	 * @param srcPath
	 *            源文件路径和名称
	 * @param disPath
	 *            目标文件路径和名称
	 * @param disWidth
	 *            目标宽度
	 * @param disHeight
	 *            目标高度
	 * @param wCutMode
	 *            目标宽度剪切模式, 可以设置常量, 如果设置为正整数则视为从0开始的偏移量. 这个参数不会和hCutMode同时应用.
	 *            方法会自动判断应用哪个.
	 * @param hCutMode
	 *            目标高度剪切模式, 可以设置常量, 如果设置为正整数则视为从0开始的偏移量. 这个参数不会和wCutMode同时应用.
	 *            方法会自动判断应用哪个.
	 * @throws IOException
	 */
	private void resizeCropImage(String srcPath, String disPath, int disWidth, int disHeight, int wCutMode, int hCutMode)
			throws IOException {
		MarvinImage image = MarvinImageIO.loadImage(srcPath); // 装载图像

		int height = image.getHeight();
		int width = image.getWidth();

		// 算法写的比较简化, 所以有点绕. 看你能力了.
		int tmpWidth = (int) Math.round((width * disHeight * 1.0 / height));
		int tmpHeight = (int) Math.round((height * disWidth * 1.0 / width));
		if (Math.abs(tmpWidth - disWidth) <= 1) // 提高一下容错度.
			tmpWidth = disWidth;
		if (Math.abs(tmpHeight - disHeight) <= 1)
			tmpHeight = disHeight;

		int x = 0, y = 0;// 图像左上角坐标,用于剪裁
		if ((height > width && tmpHeight >= disHeight) || (height <= width && tmpWidth <= disWidth)) {
			// 判断分支1: 正常情况
			// 判断分支2: 如果图比较小, 按比例缩放后宽小于目标宽度
			// 这里是高度大于宽度的情况, 应该应用hCutMode

			height = tmpHeight;
			width = disWidth;
			if (hCutMode == CUT_MODE_MIDDLE) {
				y = (height - disHeight) / 2;
			} else if (hCutMode == CUT_MODE_BUTTOM) {
				y = height - disHeight;
			} else {
				y = hCutMode;
			}
		} else {
			height = disHeight;
			width = tmpWidth;
			if (wCutMode == CUT_MODE_CENTER) {
				x = (width - disWidth) / 2;
			} else if (wCutMode == CUT_MODE_RIGHT) {
				x = width - disWidth;
			} else {
				x = wCutMode;
			}
		}
		
		MarvinImage newImage = image.clone(); // 克隆图像，确保对原图不做修改
		newImage.resize(width, height); // 设置新宽度和高度
		Crop.crop(newImage.clone(),newImage,x, y, disWidth, disHeight);
		MarvinImageIO.saveImage(newImage, disPath);// 保存图像
		logOutput("成功剪裁文件:" + srcPath.substring(srcPath.lastIndexOf("/")));

	}

	/**
	 * 按照要求缩放图片
	 * 
	 * @param srcPath
	 *            源文件路径和名称
	 * @param disPath
	 *            目标文件路径和名称
	 * @param disWidth
	 *            目标宽度
	 * @param disHeight
	 *            目标高度
	 * @param isResizeSmall
	 *            是否放大小图片
	 * @throws IOException
	 */
	private void resizeImage(String srcPath, String disPath, int disWidth, int disHeight, boolean isResizeSmall)
			throws IOException {
		MarvinImage image = MarvinImageIO.loadImage(srcPath); // 装载图像

		int height = image.getHeight();
		int width = image.getWidth();

		if (!isResizeSmall && height < disHeight && width < disWidth) {
			try {
				FileUtils.moveFile(new File(srcPath), new File(disPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			logOutput("没有缩放小图像:" + disPath);
			return;
		}

		int tmpWidth = (int) Math.round((width * disHeight * 1.0 / height));
		int tmpHeight = (int) Math.round((height * disWidth * 1.0 / width));
		if (Math.abs(tmpWidth - disWidth) <= 1) // 提高一下容错度.
			tmpWidth = disWidth;
		if (Math.abs(tmpHeight - disHeight) <= 1)
			tmpHeight = disHeight;

		if (height >= width) {
			height = tmpHeight;
			width = disWidth;
		} else {
			height = disHeight;
			width = tmpWidth;
		}
		MarvinImage newImage = null;
		newImage = image.clone(); // 克隆图像，确保对原图不做修改
		newImage.resize(width, height); // 设置新宽度和高度
		MarvinImageIO.saveImage(newImage, disPath);// 保存图像
		logOutput("成功缩放文件:" + srcPath.substring(srcPath.lastIndexOf("/")));

	}

	private void logOutput(String message) {
		System.out.println(message);
	}

	/**
	 * @return normalWidth
	 */
	public int getNormalWidth() {
		return normalWidth;
	}

	/**
	 * @param normalWidth
	 *            要设置的 normalWidth
	 */
	public void setNormalWidth(int normalWidth) {
		this.normalWidth = normalWidth;
	}

	/**
	 * @return normalHeight
	 */
	public int getNormalHeight() {
		return normalHeight;
	}

	/**
	 * @param normalHeight
	 *            要设置的 normalHeight
	 */
	public void setNormalHeight(int normalHeight) {
		this.normalHeight = normalHeight;
	}

	/**
	 * @return jpgQuality
	 */
	public float getJpgQuality() {
		return jpgQuality;
	}

	/**
	 * @param jpgQuality
	 *            要设置的 jpgQuality
	 */
	public void setJpgQuality(float jpgQuality) {
		this.jpgQuality = jpgQuality;
	}

	/**
	 * @return thumbWidth
	 */
	public int getThumbWidth() {
		return thumbWidth;
	}

	/**
	 * @param thumbWidth
	 *            要设置的 thumbWidth
	 */
	public void setThumbWidth(int thumbWidth) {
		this.thumbWidth = thumbWidth;
	}

	/**
	 * @return thumbHeight
	 */
	public int getThumbHeight() {
		return thumbHeight;
	}

	/**
	 * @param thumbHeight
	 *            要设置的 thumbHeight
	 */
	public void setThumbHeight(int thumbHeight) {
		this.thumbHeight = thumbHeight;
	}

	/**
	 * @return beginIndex
	 */
	public int getBeginIndex() {
		return beginIndex;
	}

	/**
	 * @param beginIndex
	 *            要设置的 beginIndex
	 */
	public void setBeginIndex(int beginIndex) {
		this.beginIndex = beginIndex;
	}

	/**
	 * @return filePrefix
	 */
	public String getFilePrefix() {
		return filePrefix;
	}

	/**
	 * @param filePrefix
	 *            要设置的 filePrefix
	 */
	public void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}

}
