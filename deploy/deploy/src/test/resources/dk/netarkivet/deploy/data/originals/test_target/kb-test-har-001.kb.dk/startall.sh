echo Starting all applications at: kb-test-har-001.kb.dk
#!/bin/bash
cd /home/test/test/conf/
if [ -e ./start_HarvestControllerApplication.sh ]; then 
      ./start_HarvestControllerApplication.sh
fi
