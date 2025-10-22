using System;
using System.Windows;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 激活窗口
    /// </summary>
    public partial class ActivationWindow : Window
    {
        private readonly LicenseManager _licenseManager;

        public ActivationWindow()
        {
            InitializeComponent();
            _licenseManager = new LicenseManager();
        }

        /// <summary>
        /// 激活按钮点击事件
        /// </summary>
        private async void BtnActivate_Click(object sender, RoutedEventArgs e)
        {
            string licenseKey = TxtLicenseKey.Text.Trim();

            if (string.IsNullOrWhiteSpace(licenseKey))
            {
                MessageBox.Show("请输入许可证密钥", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            // 禁用按钮，显示进度
            BtnActivate.IsEnabled = false;
            ProgressBar.Visibility = Visibility.Visible;
            LblStatus.Content = "正在激活，请稍候...";

            try
            {
                var (success, message) = await _licenseManager.ActivateAsync(licenseKey);

                if (success)
                {
                    MessageBox.Show("激活成功！", "成功", MessageBoxButton.OK, MessageBoxImage.Information);
                    DialogResult = true;
                    Close();
                }
                else
                {
                    MessageBox.Show($"激活失败：{message}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
                    LblStatus.Content = message;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"激活出错：{ex.Message}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
                LblStatus.Content = "激活失败";
            }
            finally
            {
                BtnActivate.IsEnabled = true;
                ProgressBar.Visibility = Visibility.Collapsed;
            }
        }

        /// <summary>
        /// 取消按钮点击事件
        /// </summary>
        private void BtnCancel_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = false;
            Close();
        }
    }
}

// XAML文件内容（ActivationWindow.xaml）：
/*
<Window x:Class="LicenseSystem.Client.ActivationWindow"
        xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="软件激活" Height="250" Width="500"
        WindowStartupLocation="CenterScreen" ResizeMode="NoResize">
    <Grid Margin="20">
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="*"/>
        </Grid.RowDefinitions>

        <TextBlock Grid.Row="0" Text="请输入您的许可证密钥" 
                   FontSize="14" FontWeight="Bold" Margin="0,0,0,10"/>

        <TextBox Grid.Row="1" x:Name="TxtLicenseKey" 
                 Height="30" FontSize="14" Padding="5"
                 Margin="0,0,0,10"/>

        <ProgressBar Grid.Row="2" x:Name="ProgressBar" 
                     Height="5" IsIndeterminate="True" 
                     Visibility="Collapsed" Margin="0,0,0,10"/>

        <Label Grid.Row="3" x:Name="LblStatus" 
               Content="" Foreground="Red" 
               HorizontalAlignment="Center" Margin="0,0,0,10"/>

        <StackPanel Grid.Row="4" Orientation="Horizontal" 
                    HorizontalAlignment="Right" VerticalAlignment="Bottom">
            <Button x:Name="BtnActivate" Content="激活" 
                    Width="80" Height="30" Margin="0,0,10,0"
                    Click="BtnActivate_Click"/>
            <Button x:Name="BtnCancel" Content="取消" 
                    Width="80" Height="30"
                    Click="BtnCancel_Click"/>
        </StackPanel>
    </Grid>
</Window>
*/
