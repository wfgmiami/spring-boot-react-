import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import registerServiceWorker from './registerServiceWorker';
import { BrowserRouter, Route, Switch } from 'react-router-dom';
import Versiontwo from './Versiontwo';

//import '../node_modules/bootstrap/dist/css/bootstrap.css';

const root = document.getElementById('root');

//ReactDOM.render(<App />, document.getElementById('root'));

const route = (
  <BrowserRouter>
    <Switch>
      <Route exact path="/" component={App} />
      <Route
        exact
        path="/app2"
        render={router => (
          <Versiontwo router={router} />
			)}
      />
    </Switch>
  </BrowserRouter>

);

ReactDOM.render(route, root);

registerServiceWorker();
