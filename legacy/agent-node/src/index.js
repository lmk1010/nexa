import { createHttpServer } from './http.js';
import { config } from './config.js';
import { startWorkspaceGC } from './workspace.js';

startWorkspaceGC();

const server = createHttpServer();

server.listen(config.service.port, '0.0.0.0', () => {
  console.log(
    JSON.stringify({
      level: 'info',
      msg: 'agent service started',
      service: config.service.name,
      port: config.service.port,
      model: config.agent.model,
      mock: config.agent.useMock,
    }),
  );
});

function shutdown(signal) {
  console.log(JSON.stringify({ level: 'info', msg: 'agent service stopping', signal }));
  server.close(() => {
    process.exit(0);
  });
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
