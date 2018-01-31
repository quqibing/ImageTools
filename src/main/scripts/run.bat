set rootPath=E:\temp\testimg
set srcPath=%rootPath%\src
set desPath=%rootPath%\des
set movePath=%rootPath%\move
set galleryPath=%rootPath%\gallery
java -jar imageTools.jar sync %srcPath% %desPath% -move%movePath%
java -Xms128m -Xmx768m -jar imageTools.jar filter 0.3 240*320,800*600,1024*768,1440*1024,1920*1200,3000*2000 "%desPath%"
java -jar imageTools.jar todir 20 "%desPath%\image-arNotNormal" pic n%04d
java -Xms128m -Xmx768m -jar imageTools.jar togallery %srcPath% %galleryPath% HIGH -width=1080 -height=1920 -thumbWidth=96 -thumbHeight=120 -jpgQuality=0.7 -prefix=phone
pause
