# ImageTools
      图片批量处理工具，包括按大小分类，按比例和尺寸剪裁，比较两个图片文件夹等

后续会完善说明，先列出主要功能：


 + filter: 按照图片大小过滤图片.
 + todir: 把图片按照指定数量移动到子目录中.
 + sync: 把一个目录的图片与另一个目录对比, 删除或移动第二个目录里比第一个目录中多出来的图片.
 + togallery: 把一个目录的图片可以按指定大小剪裁, 生成缩略图, 并可被Piwigo图库读取. 如果文件名中有tag，还会把tag放入图片IPTC元数据的标签中。
 
以上都是我自己处理图片的时候用到的功能。

## 用法
      
      :: 设置一些环境变量 set path
      rootPath=E:\temp\testimg
      set srcPath=%rootPath%\src
      set desPath=%rootPath%\des
      set movePath=%rootPath%\move
      set galleryPath=%rootPath%\gallery
      :: 同步两个文件夹图片 sync tow folder pictures，-move后是要移动的目标文件夹
      java -jar imageTools.jar sync %srcPath% %desPath% -move%movePath%
      :: 按图片尺寸分类到子文件夹，0.3是长宽比，不符合的图片，比如过高或者过宽的图片移动到单独的文件夹。然后按照后面的尺寸分到指定的文件夹中。尺寸按照从小到大排列，可修改。gif文件不参与分类，放到单独文件夹中。
      java -Xms128m -Xmx768m -jar imageTools.jar filter 0.3 240*320,800*600,1024*768,1440*1024,1920*1200,3000*2000 "%desPath%"
      :: 把文件按照每个文件夹20个文件的数量分配到子文件夹中，其中pic是输出目录前缀，n%04d是后缀模式。
      java -jar imageTools.jar todir 20 "%desPath%\image-arNotNormal" pic n%04d
      :: 按照指定规则剪裁并缩放图片。生成可供piwigo使用的图片和缩略图。
      java -Xms128m -Xmx768m -jar imageTools.jar togallery %srcPath% %galleryPath% HIGH -width=1080 -height=1920 -thumbWidth=96         -thumbHeight=120 -jpgQuality=0.7 -prefix=phone
