package com.aicookie;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.SanselanConstants;
import org.apache.sanselan.common.byteSources.ByteSource;
import org.apache.sanselan.common.byteSources.ByteSourceFile;
import org.apache.sanselan.formats.jpeg.JpegImageParser;
import org.apache.sanselan.formats.jpeg.JpegPhotoshopMetadata;
import org.apache.sanselan.formats.jpeg.iptc.IPTCBlock;
import org.apache.sanselan.formats.jpeg.iptc.IPTCConstants;
import org.apache.sanselan.formats.jpeg.iptc.IPTCRecord;
import org.apache.sanselan.formats.jpeg.iptc.IPTCType;
import org.apache.sanselan.formats.jpeg.iptc.JpegIptcRewriter;
import org.apache.sanselan.formats.jpeg.iptc.PhotoshopApp13Data;

public class Iptc {
	/**
	 * 是否读取图片中内嵌的缩略图
	 */
	private boolean isReadThumb = true;

	public boolean isReadThumb() {
		return isReadThumb;
	}

	public void setReadThumb(boolean isReadThumb) {
		this.isReadThumb = isReadThumb;
	}

	/**
	 * 移除所有Iptc
	 * 
	 * @param imgPath
	 * @throws IOException
	 * @throws ImageReadException
	 * @throws ImageWriteException
	 */
	public void removeAllIptc(String imgPath) throws IOException,
			ImageReadException, ImageWriteException {
		File imageFile = new File(imgPath);
		String tempPath = imageFile.getAbsolutePath() + (new Date()).getTime()
				+ Math.round(Math.random() * 5) + ".tmp";
		ByteSource byteSource = new ByteSourceFile(imageFile);
		Map<String, Boolean> params = new HashMap<String, Boolean>();
		params.put(SanselanConstants.PARAM_KEY_READ_THUMBNAILS, isReadThumb);

		@SuppressWarnings("unused")
		JpegPhotoshopMetadata metadata = new JpegImageParser()
				.getPhotoshopMetadata(byteSource, params);

		File noIptcFile = new File(tempPath);
		{
			OutputStream os = null;
			try {
				os = new FileOutputStream(noIptcFile);
				os = new BufferedOutputStream(os);
				new JpegIptcRewriter().removeIPTC(byteSource, os);
			} finally {
				os.close();
				os = null;
			}

		}
		imageFile.delete();
		noIptcFile.renameTo(imageFile);

	}

	/**
	 * 获取Metadata
	 * 
	 * @param imgPath
	 * @param isReadThumb
	 * @return
	 * @throws ImageReadException
	 * @throws IOException
	 */
	public JpegPhotoshopMetadata getMetadata(String imgPath)
			throws ImageReadException, IOException {
		ByteSource byteSource = new ByteSourceFile(new File(imgPath));
		Map<String, Boolean> params = new HashMap<String, Boolean>();
		params.put(SanselanConstants.PARAM_KEY_READ_THUMBNAILS, isReadThumb);
		return new JpegImageParser().getPhotoshopMetadata(byteSource, params);
	}

	/**
	 * 更新Iptc,保留与更新列表中类型不冲突的数据,但相同类型的数据会全部删除
	 * 
	 * @param imgPath
	 * @param newIptc
	 * @throws IOException
	 * @throws ImageReadException
	 * @throws ImageWriteException
	 */
	@SuppressWarnings("unchecked")
	public void updateIptc(String imgPath, List<IPTCRecord> newIptc)
			throws IOException, ImageReadException, ImageWriteException {
		File imgFile = new File(imgPath);
		ByteSource byteSource = new ByteSourceFile(imgFile);
		Map<String, Boolean> params = new HashMap<String, Boolean>();
		params.put(SanselanConstants.PARAM_KEY_READ_THUMBNAILS, isReadThumb);
		//获取Metadata
		JpegPhotoshopMetadata metadata = new JpegImageParser()
				.getPhotoshopMetadata(byteSource, params);
		
		//获取记录
		List<IPTCBlock> newBlocks = new ArrayList<IPTCBlock>();
		List<IPTCRecord> oldRecords = null;
		List<IPTCRecord> newRecords = new ArrayList<IPTCRecord>();
		//如果原来有信息, 则要添加原来的不同信息.
		if (metadata != null && metadata.photoshopApp13Data != null) {
			if(metadata.photoshopApp13Data.getRawBlocks() != null)
				newBlocks = metadata.photoshopApp13Data.getNonIptcBlocks();
			oldRecords = metadata.photoshopApp13Data.getRecords();

			HashSet<Integer> newIptcTypes = new HashSet<Integer>();
			for (int i = 0; i < newIptc.size(); i++)
				newIptcTypes.add(newIptc.get(i).iptcType.type);
			for (int j = 0; j < oldRecords.size(); j++) {
				IPTCRecord record = (IPTCRecord) oldRecords.get(j);
				if (!newIptcTypes.contains(record.iptcType.type))
					newRecords.add(record);
			}
		}
		newRecords.addAll(newIptc);
		PhotoshopApp13Data newData = new PhotoshopApp13Data(newRecords,
				newBlocks);
		File newIptcFile = getTempFile(imgFile);
		{
			OutputStream os = null;
			try {
				os = new FileOutputStream(newIptcFile);
				os = new BufferedOutputStream(os);
				new JpegIptcRewriter().writeIPTC(byteSource, os, newData);

			} finally {
				os.close();
				os = null;
			}
		}
		imgFile.delete();
		newIptcFile.renameTo(imgFile);

	}

	private File getTempFile(File imgFile) {
		String path = imgFile.getPath().replaceAll("\\\\", "/");
		String pathname = path.substring(0,path.lastIndexOf("/")+1) + (new Date()).getTime()
				+ Math.round(Math.random() * 10) + ".tmp";
		return new File(pathname);
	}

	/**
	 * 添加Iptc,保留原来所有的Itpc数据
	 * 
	 * @param imgPath
	 * @param newIptc
	 * @throws IOException
	 * @throws ImageReadException
	 * @throws ImageWriteException
	 */
	public void addIptc(String imgPath, List<IPTCRecord> newIptc)
			throws IOException, ImageReadException, ImageWriteException {
		File imgFile = new File(imgPath);
		ByteSource byteSource = new ByteSourceFile(imgFile);
		Map<String, Boolean> params = new HashMap<String, Boolean>();
		params.put(SanselanConstants.PARAM_KEY_READ_THUMBNAILS, isReadThumb);
		JpegPhotoshopMetadata metadata = new JpegImageParser()
				.getPhotoshopMetadata(byteSource, params);
		@SuppressWarnings("unchecked")
		List<IPTCBlock> newBlocks = metadata.photoshopApp13Data
				.getNonIptcBlocks();
		@SuppressWarnings("unchecked")
		List<IPTCRecord> newRecords = metadata.photoshopApp13Data.getRecords();
		newRecords.addAll(newIptc);
		PhotoshopApp13Data newData = new PhotoshopApp13Data(newRecords,
				newBlocks);

		String tempPath = imgFile.getPath() + (new Date()).getTime()
				+ Math.round(Math.random() * 5) + ".tmp";
		File newIptcFile = new File(tempPath);
		{
			OutputStream os = null;
			try {
				os = new FileOutputStream(newIptcFile);
				os = new BufferedOutputStream(os);
				new JpegIptcRewriter().writeIPTC(byteSource, os, newData);

			} finally {
				os.close();
				os = null;
			}
		}
		imgFile.delete();
		newIptcFile.renameTo(imgFile);

	}

	/**
	 * 移除Iptc,移除参数列表中的类型
	 * 
	 * @param imgPath
	 * @param newIptc
	 * @throws IOException
	 * @throws ImageReadException
	 * @throws ImageWriteException
	 */
	public void removeIptc(String imgPath, List<IPTCType> removeIptcList)
			throws IOException, ImageReadException, ImageWriteException {
		File imgFile = new File(imgPath);
		ByteSource byteSource = new ByteSourceFile(imgFile);
		Map<String, Boolean> params = new HashMap<String, Boolean>();
		params.put(SanselanConstants.PARAM_KEY_READ_THUMBNAILS, isReadThumb);
		JpegPhotoshopMetadata metadata = new JpegImageParser()
				.getPhotoshopMetadata(byteSource, params);
		@SuppressWarnings("unchecked")
		List<IPTCBlock> newBlocks = metadata.photoshopApp13Data
				.getNonIptcBlocks();
		@SuppressWarnings("unchecked")
		List<IPTCRecord> oldRecords = metadata.photoshopApp13Data.getRecords();

		List<IPTCRecord> newRecords = new ArrayList<IPTCRecord>();
		HashSet<Integer> removeIptcTypes = new HashSet<Integer>();
		for (int i = 0; i < removeIptcList.size(); i++)
			removeIptcTypes.add(removeIptcList.get(i).type);
		for (int j = 0; j < oldRecords.size(); j++) {
			IPTCRecord record = (IPTCRecord) oldRecords.get(j);
			if (!removeIptcTypes.contains(record.iptcType.type))
				newRecords.add(record);
		}

		PhotoshopApp13Data newData = new PhotoshopApp13Data(newRecords,
				newBlocks);

		String tempPath = imgFile.getPath() + (new Date()).getTime()
				+ Math.round(Math.random() * 5) + ".tmp";
		File newIptcFile = new File(tempPath);
		{
			OutputStream os = null;
			try {
				os = new FileOutputStream(newIptcFile);
				os = new BufferedOutputStream(os);
				new JpegIptcRewriter().writeIPTC(byteSource, os, newData);

			} finally {
				os.close();
				os = null;
			}
		}
		imgFile.delete();
		newIptcFile.renameTo(imgFile);

	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ImageWriteException
	 * @throws ImageReadException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws ImageReadException,
			ImageWriteException, IOException {
		System.out.println("运行了");
		String path = "M:/Android/test/test.jpg";
		// new Iptc().removeAllIptc("M:/Android/test/test.jpg");
		List lst = new ArrayList();
		lst.add(new IPTCRecord(IPTCConstants.IPTC_TYPE_OBJECT_NAME, new String(
				"GG的的".getBytes(), "ISO-8859-1")));
		lst.add(new IPTCRecord(IPTCConstants.IPTC_TYPE_CAPTION_ABSTRACT,
				new String("描述的".getBytes(), "ISO-8859-1")));
		lst.add(new IPTCRecord(IPTCConstants.IPTC_TYPE_KEYWORDS, new String(
				"mykey我的".getBytes(), "ISO-8859-1")));
		new Iptc().updateIptc(path, lst);
	}

}
