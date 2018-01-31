package com.aicookie;

public class Run {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			error();
			return;
		}
		String func = args[0];
		String[] argAry = new String[args.length - 1];
		for (int i = 0; i < args.length - 1; i++)
			argAry[i] = args[i + 1];
		if (func.equals("filter"))
			ImageFilter.main(argAry);
		else if (func.equals("todir"))
			ImageToDir.main(argAry);
		else if (func.equals("sync"))
			SyncDeleteFiles.main(argAry);
		else if (func.equals("togallery"))
			CreateGallery.main(argAry);
		else
			error();
	}

	private static void error() {
		System.out.println("未指定功能名称! 现在可用的功能有:");
		System.out.println("filter: 按照图片大小过滤图片.");
		System.out.println("todir: 把图片按照指定数量移动到子目录中.");
		System.out.println("sync: 把一个目录的图片与另一个目录对比, 删除或移动第二个目录里比第一个目录中多出来的图片.");
		System.out.println("togallery: 把一个目录的图片可以按指定大小剪裁, 生成缩略图, 并可被Piwigo图库读取. ");
	}
}
