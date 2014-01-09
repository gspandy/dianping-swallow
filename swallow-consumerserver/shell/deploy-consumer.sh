#!/bin/bash
##########################################################################################################################################
#  在dev/alpha/qa环境下的consumerserver部署方法(product环境使用群英的脚本，它会遍历多台swallow机器批量发布)：
#  (1)打包tar之后，放到ftp，路径为ftp://10.1.1.189/arch/<当天日期>/<Mode>/swallow-consumerserver-withDependencies.tar
#  (2)到相应机器(dev/alpha/qa)的 $AppRootDir 目录下，执行sh deploy-consumer.sh <Mode>即可。
#
##########################################################################################################################################


##################################################
# Some utility functions
##################################################
usage(){
    echo "  Usage:"
    echo "     use '$0 dev/alpha/qa'."
    exit 1
}
pause(){
   read -p "$*"
}

#环境变量
source /etc/profile
FtpServer="ftp://10.1.1.189/arch"
Datedir=$(date +%Y%m%d)
TarFile="swallow-consumerserver-withDependencies.tar"
Mode=$1
FileFtpAddress="$FtpServer/$Datedir/$Mode/$TarFile"
if [ "$Mode" = "dev" ]; then
    UserDir="/home/wukezhu"
    MasterIp="192.168.8.21"
elif [ "$Mode" = "alpha" ]; then
    UserDir="/data/swallow"
    MasterIp="192.168.7.41"
elif [ "$Mode" = "qa" ]; then
    UserDir="/data/webapps"
    MasterIp="192.168.213.143"
else
    echo "Your input is not corrent!"
    usage
    exit 1
fi
MasterDir="swallow-consumerserver-master"
SlaveDir="swallow-consumerserver-slave"

cd $UserDir

#下载，备份，解压
echo "================ Downloading, backup and tar file ================"
rm -f $TarFile
echo "Downloading file : $FileFtpAddress"
wget $FileFtpAddress
rm -rf $MasterDir-backup && cp -r $MasterDir $MasterDir-backup
rm -rf $MasterDir && mkdir $MasterDir && tar xf $TarFile -C $MasterDir
echo "Backuped master dir."
rm -rf $SlaveDir-backup && cp -r $SlaveDir $SlaveDir-backup
rm -rf $SlaveDir && mkdir $SlaveDir && tar xf $TarFile -C $SlaveDir
echo "Backuped slave dir."

#重启master
echo ""
pause 'Press [Enter] key to restart master ...'
cd $UserDir/$MasterDir
sh swallow.sh restart master

#重启slave
echo ""
pause 'Press [Enter] key to restart slave ...'
cd $UserDir/$SlaveDir
sh swallow.sh restart slave $MasterIp
