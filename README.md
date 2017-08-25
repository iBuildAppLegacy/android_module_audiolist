
Use our code to save yourself time on cross-platform, cross-device and cross OS version development and testing
- The AudioList widget is designed to listen to your favorite music.
You can comment on your impressions of what you heard and put like.
Authorization via facebook, twitter or create a new account is implemented.

# XML Structure declaration

# Tags: 

- App_name - the name of the mobile application. 
- Allowsharing - Ability to repost on your page in social networks
- Allowcomments - Ability to leave comments about the melody heard.
- Cover_image - cover
- Colorskin - this is the basic color scheme. Contains 5 elements (color [1-5]). Each widget can set colors for elements of the interface using color scheme in any order, but generally color1-background color, color3-titles color, color4-font color, color5-date or price color.
- Track - this is a root tag about track
- Title - name of track
- Description - description of track
- Cover_image - cover
- Permalink_url - link to track
- Stream_url - stream reference
- Id - id track

# Example:

         <data>
         <app_name>BigApp</app_name>
         <allowsharing>on</allowsharing>
         <allowcomments>on</allowcomments>
         <cover_image><![CDATA[]]></cover_image>
         <colorskin>
            <color1><![CDATA[#c2e793]]></color1>
            <color2><![CDATA[#2d910b]]></color2>
            <color3><![CDATA[#225112]]></color3>
            <color4><![CDATA[#313e20]]></color4>
            <color5><![CDATA[#2d910b]]></color5>
            <color6><![CDATA[rgba(255,255,255,0.2)]]></color6>
            <color7><![CDATA[rgba(255,255,255,0.15)]]></color7>
            <color8><![CDATA[rgba(0,0,0,0.3)]]></color8>
            <isLight><![CDATA[1]]></isLight>
         </colorskin>
         <track>
             <title><![CDATA[Noisestorm - Somewhere In Time]]></title>
             <description><![CDATA[sdhjgfdg]]></description>
             <cover_image><![CDATA[]]></cover_image>
             <permalink_url><![CDATA[http://soundcloud.com/user-119509475-324743990/noisestorm-somewhere-in-time]]></permalink_url>
             <stream_url><![CDATA[https://api.soundcloud.com/tracks/245542309/stream]]></stream_url>
             <id><![CDATA[1529493019629435]]></id>
         </track>
         <track>
             <title><![CDATA[Nimff - Jersey]]></title>
             <description><![CDATA[sdhjgfdg]]></description>
             <cover_image><![CDATA[]]></cover_image>
             <permalink_url><![CDATA[http://soundcloud.com/user-119509475-324743990/nimff-jersey]]></permalink_url>
             <stream_url><![CDATA[https://api.soundcloud.com/tracks/245542308/stream]]></stream_url>
             <id><![CDATA[1529493019709659]]></id>
         </track>
         <track>
             <title><![CDATA[PLS stream]]></title>
             <description><![CDATA[]]></description>
             <cover_image><![CDATA[]]></cover_image>
             <permalink_url><![CDATA[http://www.internet-radio.com/servers/tools/playlistgenerator/?u=http://us1.internet-radio.com:11094/listen.pls&t=.pls]]></permalink_url>
             <stream_url><![CDATA[http://us1.internet-radio.com:11094/]]></stream_url>
             <id><![CDATA[1529493359853551]]></id>
         </track>
   
         </data>
