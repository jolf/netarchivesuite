echo Starting all applications at: kb-test-har-002.kb.dk
#!/bin/bash
cd /home/test/TEST/conf/
if [ -e ./start_HarvestControllerApplication_low.sh ]; then 
      ./start_HarvestControllerApplication_low.sh
fi
if [ -e ./start_HarvestControllerApplication_high.sh ]; then 
      ./start_HarvestControllerApplication_high.sh
fi
