import React from 'react';
import ReactDOM from 'react-dom';
import injectTapEventPlugin from 'react-tap-event-plugin';
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';
import './index.css';
import registerServiceWorker from './registerServiceWorker';
import Logger from './pages/Logger';

injectTapEventPlugin();
function App() {
  return (
    <MuiThemeProvider>
      <Logger />
    </MuiThemeProvider>
  );
}

ReactDOM.render(<App />, document.getElementById('root'));
registerServiceWorker();
