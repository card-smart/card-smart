#!/bin/bash

while true
do
	echo | openssl s_client -connect localhost:3000 -cert /home/veronika/timestamping-server/certs/public/cert_1.pem -key /home/veronika/timestamping-server/certs/private/private_key_1.pem &
	echo | openssl s_client -connect localhost:3001 -cert /home/veronika/timestamping-server/certs/public/cert_2.pem -key /home/veronika/timestamping-server/certs/private/private_key_2.pem &
#	echo | openssl s_client -connect localhost:3002 -cert /home/veronika/timestamping-server/certs/public/cert_3.pem -key /home/veronika/timestamping-server/certs/private/private_key_3.pem &
done
